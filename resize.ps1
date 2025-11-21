Add-Type -AssemblyName System.Drawing
$icon = [System.Drawing.Image]::FromFile("C:\src\projects\flashcard\icon.png")
$sizes = @{"mipmap-mdpi"=108;"mipmap-hdpi"=162;"mipmap-xhdpi"=216;"mipmap-xxhdpi"=324;"mipmap-xxxhdpi"=432}
foreach($f in $sizes.Keys){
  $s=$sizes[$f]
  $n=New-Object System.Drawing.Bitmap($s,$s)
  $g=[System.Drawing.Graphics]::FromImage($n)
  $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.CompositingQuality=[System.Drawing.Drawing2D.CompositingQuality]::HighQuality
  $g.DrawImage($icon,0,0,$s,$s)
  $n.Save("C:\src\projects\flashcard\android\app\src\main\res\$f\ic_launcher.png")
  $g.Dispose()
  $n.Dispose()
  Write-Host "Created $f ($s x $s)"
}
$fg=New-Object System.Drawing.Bitmap(1296,1296)
$g=[System.Drawing.Graphics]::FromImage($fg)
$g.Clear([System.Drawing.Color]::Transparent)
$g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.CompositingQuality=[System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$offset=108
$iconSize=1080
$g.DrawImage($icon,$offset,$offset,$iconSize,$iconSize)
$fg.Save("C:\src\projects\flashcard\android\app\src\main\res\drawable\ic_launcher_foreground.png")
$g.Dispose()
$fg.Dispose()
$icon.Dispose()
Write-Host "All done! High quality with anti-aliasing"
