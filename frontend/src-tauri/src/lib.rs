use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::sync::Mutex;
use tauri::{Manager, RunEvent};

#[cfg(windows)]
use std::os::windows::process::CommandExt;

pub struct BackendProcess(Mutex<Option<Child>>);

fn normalize_path(p: &Path) -> PathBuf {
    let s = p.to_string_lossy();
    let s: &str = &s;
    let s = s.strip_prefix("\\\\?\\").unwrap_or(s);
    PathBuf::from(s)
}

fn spawn_backend(app: &tauri::AppHandle) -> Option<Child> {
    let resource_dir = app.path().resource_dir().ok()?;
    let base = resource_dir.join("resources");
    let java = normalize_path(&base.join("jre").join("bin").join("java.exe"));
    let jar = normalize_path(&base.join("backend.jar"));
    if !java.is_file() || !jar.is_file() {
        eprintln!("backend resources not found (dev mode?)");
        return None;
    }

    let mut cmd = Command::new(&java);
    cmd.arg("-jar")
        .arg(&jar)
        .arg("--server.address=127.0.0.1")
        .arg("--server.port=8111");

    // No console window for the child (CREATE_NO_WINDOW).
    #[cfg(windows)]
    cmd.creation_flags(0x08000000);

    // Redirect backend logs to a file so they can be inspected later.
    if let Ok(log_dir) = app.path().app_log_dir() {
        let _ = std::fs::create_dir_all(&log_dir);
        let log_file = log_dir.join("backend.log");
        if let Ok(out) = std::fs::File::create(&log_file) {
            if let Ok(err) = out.try_clone() {
                cmd.stdout(Stdio::from(out));
                cmd.stderr(Stdio::from(err));
            }
        }
    }

    cmd.spawn().ok()
}

fn kill_backend(app: &tauri::AppHandle) {
    if let Some(mut child) = app.state::<BackendProcess>().0.lock().unwrap().take() {
        let _ = child.kill();
        let _ = child.wait();
    }
}

pub fn run() {
    let app = tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .manage(BackendProcess(Mutex::new(None)))
        .setup(|app| {
            let handle = app.handle().clone();
            let child = spawn_backend(&handle);
            if child.is_some() {
                eprintln!("backend started");
            }
            *app.state::<BackendProcess>().0.lock().unwrap() = child;
            Ok(())
        })
        .build(tauri::generate_context!())
        .expect("error while building tauri application");

    app.run(|app_handle, event| {
        if let RunEvent::Exit = event {
            kill_backend(app_handle);
        }
    });
}
