# Captures the screen once per auto-tour step, naming each image by the map ids the server is
# showing at that moment. Run this beside the game window while /warp tour auto runs; stop the
# tour (type "stop" in game chat) and this script ends itself.
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
$positionFile = Join-Path $PSScriptRoot "tour-position.txt"
$outDir = Join-Path $PSScriptRoot "tour-captures"
New-Item -ItemType Directory -Force $outDir | Out-Null
$last = ""
Write-Host "Capturing tour screenshots to $outDir - start /warp tour auto in game."
while ($true) {
    Start-Sleep -Milliseconds 500
    if (-not (Test-Path $positionFile)) { continue }
    $pos = (Get-Content $positionFile -TotalCount 1).Trim()
    if ($pos -eq "stopped") { Write-Host "Tour stopped; done."; break }
    if ($pos -eq $last -or $pos -eq "") { continue }
    # Let the map finish fading in before the shot.
    Start-Sleep -Milliseconds 2500
    $bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $bitmap = New-Object System.Drawing.Bitmap $bounds.Width, $bounds.Height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
    $name = ($pos -replace ";", "_") + ".png"
    $bitmap.Save((Join-Path $outDir $name), [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose(); $bitmap.Dispose()
    Write-Host "captured $name"
    $last = $pos
}
