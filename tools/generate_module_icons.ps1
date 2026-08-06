Add-Type -AssemblyName System.Drawing

$outputDirectory = Join-Path $PSScriptRoot '..\src\main\resources\assets\riftgun\textures\item\modules'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

function New-ModuleIcon {
    param(
        [string] $Name,
        [System.Drawing.Color] $Accent,
        [scriptblock] $DrawSymbol
    )

    $bitmap = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $transparent = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) { $bitmap.SetPixel($x, $y, $transparent) }
    }

    $outer = [System.Drawing.Color]::FromArgb(255, 30, 33, 38)
    $edge = [System.Drawing.Color]::FromArgb(255, 62, 67, 73)
    $inner = [System.Drawing.Color]::FromArgb(255, 18, 21, 25)
    $pin = [System.Drawing.Color]::FromArgb(255, 128, 134, 139)
    for ($y = 1; $y -le 14; $y++) {
        for ($x = 1; $x -le 14; $x++) { $bitmap.SetPixel($x, $y, $outer) }
    }
    for ($i = 2; $i -le 13; $i++) {
        $bitmap.SetPixel($i, 2, $edge); $bitmap.SetPixel($i, 13, $edge)
        $bitmap.SetPixel(2, $i, $edge); $bitmap.SetPixel(13, $i, $edge)
    }
    for ($y = 3; $y -le 12; $y++) {
        for ($x = 3; $x -le 12; $x++) { $bitmap.SetPixel($x, $y, $inner) }
    }
    foreach ($x in 5, 8, 10) { $bitmap.SetPixel($x, 1, $pin); $bitmap.SetPixel($x, 14, $pin) }
    foreach ($y in 5, 8, 10) { $bitmap.SetPixel(1, $y, $pin); $bitmap.SetPixel(14, $y, $pin) }

    & $DrawSymbol $bitmap $Accent
    $path = Join-Path $outputDirectory ($Name + '.png')
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function Set-Pixel([System.Drawing.Bitmap] $Bitmap, [int] $X, [int] $Y, [System.Drawing.Color] $Color) {
    $Bitmap.SetPixel($X, $Y, $Color)
}

New-ModuleIcon 'coordinate_override_module' ([System.Drawing.Color]::FromArgb(255, 116, 217, 232)) {
    param($b, $c)
    foreach ($p in @(@(7,4),@(8,4),@(7,5),@(8,5),@(7,10),@(8,10),@(7,11),@(8,11),
                     @(4,7),@(4,8),@(5,7),@(5,8),@(10,7),@(10,8),@(11,7),@(11,8))) {
        Set-Pixel $b $p[0] $p[1] $c
    }
    foreach ($p in @(@(6,6),@(9,6),@(6,9),@(9,9))) { Set-Pixel $b $p[0] $p[1] $c }
}

New-ModuleIcon 'reservoir_expansion_module' ([System.Drawing.Color]::FromArgb(255, 91, 173, 235)) {
    param($b, $c)
    for ($x = 5; $x -le 10; $x++) { Set-Pixel $b $x 4 $c; Set-Pixel $b $x 11 $c }
    for ($y = 5; $y -le 10; $y++) { Set-Pixel $b 5 $y $c; Set-Pixel $b 10 $y $c }
    for ($x = 6; $x -le 9; $x++) { Set-Pixel $b $x 9 $c; Set-Pixel $b $x 10 $c }
    Set-Pixel $b 7 7 $c; Set-Pixel $b 8 6 $c; Set-Pixel $b 9 7 $c
}

New-ModuleIcon 'passive_transit_module' ([System.Drawing.Color]::FromArgb(255, 142, 208, 129)) {
    param($b, $c)
    foreach ($p in @(@(4,4),@(5,4),@(10,4),@(11,4))) { Set-Pixel $b $p[0] $p[1] $c }
    for ($y = 5; $y -le 11; $y++) { for ($x = 4; $x -le 11; $x++) { Set-Pixel $b $x $y $c } }
    $dark = [System.Drawing.Color]::FromArgb(255, 47, 72, 47)
    Set-Pixel $b 6 7 $dark; Set-Pixel $b 9 7 $dark
    for ($x = 6; $x -le 9; $x++) { Set-Pixel $b $x 9 $dark; Set-Pixel $b $x 10 $dark }
    Set-Pixel $b 7 9 $c; Set-Pixel $b 8 9 $c
}

New-ModuleIcon 'hostile_transit_module' ([System.Drawing.Color]::FromArgb(255, 220, 118, 95)) {
    param($b, $c)
    for ($y = 4; $y -le 11; $y++) { for ($x = 4; $x -le 11; $x++) { Set-Pixel $b $x $y $c } }
    $dark = [System.Drawing.Color]::FromArgb(255, 53, 61, 43)
    Set-Pixel $b 5 6 $dark; Set-Pixel $b 6 6 $dark; Set-Pixel $b 9 6 $dark; Set-Pixel $b 10 6 $dark
    Set-Pixel $b 7 8 $dark; Set-Pixel $b 8 8 $dark
    for ($x = 6; $x -le 9; $x++) { Set-Pixel $b $x 10 $dark }
    Set-Pixel $b 5 11 $dark; Set-Pixel $b 10 11 $dark
}

New-ModuleIcon 'boss_transit_module' ([System.Drawing.Color]::FromArgb(255, 167, 123, 214)) {
    param($b, $c)
    foreach ($p in @(@(4,3),@(5,4),@(10,4),@(11,3),@(5,5),@(6,5),@(7,5),@(8,5),@(9,5),@(10,5),
                     @(5,6),@(6,6),@(7,6),@(8,6),@(9,6),@(10,6),@(6,7),@(7,7),@(8,7),@(9,7),
                     @(6,8),@(7,8),@(8,8),@(9,8),@(7,9),@(8,9),@(7,10),@(8,10))) {
        Set-Pixel $b $p[0] $p[1] $c
    }
    $eye = [System.Drawing.Color]::FromArgb(255, 230, 210, 255)
    Set-Pixel $b 6 6 $eye; Set-Pixel $b 9 6 $eye
}

New-ModuleIcon 'surface_range_amplifier' ([System.Drawing.Color]::FromArgb(255, 227, 183, 92)) {
    param($b, $c)
    for ($y = 4; $y -le 11; $y++) { Set-Pixel $b 4 $y $c; Set-Pixel $b 11 $y $c }
    Set-Pixel $b 5 4 $c; Set-Pixel $b 5 11 $c; Set-Pixel $b 10 4 $c; Set-Pixel $b 10 11 $c
    for ($y = 6; $y -le 9; $y++) { Set-Pixel $b 6 $y $c; Set-Pixel $b 9 $y $c }
    Set-Pixel $b 7 7 $c; Set-Pixel $b 8 7 $c; Set-Pixel $b 7 8 $c; Set-Pixel $b 8 8 $c
}
