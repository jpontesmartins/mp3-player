use std::path::{Path, PathBuf};
use std::process::{Child, Command};
use std::sync::Mutex;
use tauri::{Manager, RunEvent};

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
    Command::new(&java)
        .arg("-jar")
        .arg(&jar)
        .arg("--server.address=127.0.0.1")
        .arg("--server.port=8080")
        .spawn()
        .ok()
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
