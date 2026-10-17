Set WshShell = CreateObject("WScript.Shell")
WshShell.Run "cmd /c ""cd /d C:\Users\ramna\OneDrive\ドキュメント\Projects\sonora-ai && netstat -ano | findstr :8080 >nul || python -m uvicorn main:app --app-dir backend --host 0.0.0.0 --port 8080""", 0, False
Set WshShell = Nothing
