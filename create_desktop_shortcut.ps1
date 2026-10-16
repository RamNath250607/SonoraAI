$WshShell = New-Object -ComObject WScript.Shell
$DesktopPath = "C:\Users\ramna\OneDrive\Desktop"
$ShortcutPath = Join-Path $DesktopPath "SONORA AI.lnk"
$Shortcut = $WshShell.CreateShortcut($ShortcutPath)

if (Test-Path "C:\Program Files\Google\Chrome\Application\chrome.exe") {
    $Shortcut.TargetPath = "C:\Program Files\Google\Chrome\Application\chrome.exe"
    $Shortcut.Arguments = "--app=http://localhost:3000 --window-size=1280,840"
} elseif (Test-Path "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe") {
    $Shortcut.TargetPath = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
    $Shortcut.Arguments = "--app=http://localhost:3000 --window-size=1280,840"
} else {
    $Shortcut.TargetPath = "http://localhost:3000"
}

$Shortcut.Description = "SONORA AI - Smart Voice & Text Music Player"
$Shortcut.WorkingDirectory = "c:\Users\ramna\OneDrive\ドキュメント\Projects\sonora-ai"
$Shortcut.Save()
Write-Output "Shortcut created successfully at $ShortcutPath"
