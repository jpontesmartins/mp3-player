# Referências — ofy-005

## Exemplos de consulta (derivados do código)
| Consulta | Efeito |
|---|---|
| `queen` | texto livre em title/artist/album/genre/year/track |
| `<genre> == rock && <year> > 2000` | gênero "rock" E ano maior que 2000 |
| `<artist> == Queen, Beatles` | artista igual a qualquer um dos valores |
| `<artist> != Queen, Beatles` | artista diferente de todos |
| `rock \|\| pop` | texto livre rock OU pop |
| `<track> <= 5` | faixa numérica até 5 |
