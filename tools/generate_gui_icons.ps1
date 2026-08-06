Add-Type -AssemblyName System.Drawing

$outputDirectory = Join-Path $PSScriptRoot '..\src\main\resources\assets\riftgun\textures\gui\sprites\icons'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$colors = @{
    PanelRaised = [System.Drawing.Color]::FromArgb(255, 37, 39, 45)
    Panel       = [System.Drawing.Color]::FromArgb(255, 27, 29, 34)
    Muted       = [System.Drawing.Color]::FromArgb(255, 167, 163, 156)
    Ice         = [System.Drawing.Color]::FromArgb(255, 156, 201, 216)
    IceDark     = [System.Drawing.Color]::FromArgb(255, 65, 103, 117)
    Portal      = [System.Drawing.Color]::FromArgb(255, 100, 212, 93)
    Danger      = [System.Drawing.Color]::FromArgb(255, 225, 132, 121)
    Warning     = [System.Drawing.Color]::FromArgb(255, 226, 182, 107)
    Passive     = [System.Drawing.Color]::FromArgb(255, 167, 215, 155)
    Hostile     = [System.Drawing.Color]::FromArgb(255, 217, 130, 100)
    Boss        = [System.Drawing.Color]::FromArgb(255, 179, 138, 216)
    StarOn      = [System.Drawing.Color]::FromArgb(255, 255, 215, 102)
    StarOff     = [System.Drawing.Color]::FromArgb(255, 212, 170, 82)
}

function New-Icon([string] $Name, [scriptblock] $Draw) {
    $bitmap = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $bitmap.MakeTransparent()
    & $Draw $bitmap
    $bitmap.Save((Join-Path $outputDirectory ($Name + '.png')), [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function Fill-Rect($Bitmap, [int] $X, [int] $Y, [int] $Width, [int] $Height, $Color) {
    for ($py = $Y; $py -lt $Y + $Height; $py++) {
        for ($px = $X; $px -lt $X + $Width; $px++) { $Bitmap.SetPixel($px, $py, $Color) }
    }
}

function Outline-Rect($Bitmap, [int] $X, [int] $Y, [int] $Width, [int] $Height, $Color) {
    Fill-Rect $Bitmap $X $Y $Width 1 $Color
    Fill-Rect $Bitmap $X ($Y + $Height - 1) $Width 1 $Color
    Fill-Rect $Bitmap $X $Y 1 $Height $Color
    Fill-Rect $Bitmap ($X + $Width - 1) $Y 1 $Height $Color
}

function Draw-Bucket($Bitmap, $Color) {
    Fill-Rect $Bitmap 3 4 2 2 $Color
    Fill-Rect $Bitmap 4 5 2 7 $Color
    Fill-Rect $Bitmap 5 11 6 2 $Color
    Fill-Rect $Bitmap 10 5 2 7 $Color
    Fill-Rect $Bitmap 5 4 6 2 $Color
}

New-Icon 'bucket_on'  { param($b) Draw-Bucket $b $colors.Ice }
New-Icon 'bucket_off' { param($b) Draw-Bucket $b $colors.Muted }

function Draw-Drain($Bitmap, $Color) {
    Fill-Rect $Bitmap 7 3 2 2 $Color
    Fill-Rect $Bitmap 6 5 4 3 $Color
    Fill-Rect $Bitmap 5 8 6 3 $Color
    Fill-Rect $Bitmap 6 11 4 2 $Color
    Fill-Rect $Bitmap 3 7 2 1 $colors.Muted
    Fill-Rect $Bitmap 11 7 2 1 $colors.Muted
}

New-Icon 'drain_on'  { param($b) Draw-Drain $b $colors.Danger }
New-Icon 'drain_off' { param($b) Draw-Drain $b $colors.Muted }

New-Icon 'placement_smart' {
    param($b)
    Fill-Rect $b 7 3 1 9 $colors.Ice
    Fill-Rect $b 3 7 9 1 $colors.Ice
    Fill-Rect $b 6 6 3 3 $colors.PanelRaised
    Fill-Rect $b 10 4 2 1 $colors.Ice
    Fill-Rect $b 11 5 1 2 $colors.Ice
}
New-Icon 'placement_front' {
    param($b)
    Outline-Rect $b 6 3 6 9 $colors.Ice
    Fill-Rect $b 4 7 2 1 $colors.Muted
}
New-Icon 'placement_surface' {
    param($b)
    Fill-Rect $b 3 3 1 10 $colors.Muted
    Outline-Rect $b 5 4 7 8 $colors.Ice
}

function Draw-Prediction($Bitmap, $Color) {
    Fill-Rect $Bitmap 2 9 1 1 $Color
    Fill-Rect $Bitmap 5 8 1 1 $Color
    Fill-Rect $Bitmap 8 7 1 1 $Color
    Outline-Rect $Bitmap 10 5 3 7 $Color
}
New-Icon 'prediction_on'  { param($b) Draw-Prediction $b $colors.Ice }
New-Icon 'prediction_off' { param($b) Draw-Prediction $b $colors.Muted }

New-Icon 'configure_gun' {
    param($b)
    Outline-Rect $b 3 3 10 3 $colors.Ice
    Outline-Rect $b 3 9 10 3 $colors.Ice
    Fill-Rect $b 5 2 2 5 $colors.PanelRaised
    Fill-Rect $b 5 3 2 3 $colors.Ice
    Fill-Rect $b 9 8 2 5 $colors.PanelRaised
    Fill-Rect $b 9 9 2 3 $colors.Ice
}
New-Icon 'module_bay' {
    param($b)
    Outline-Rect $b 2 2 11 11 $colors.Warning
    Fill-Rect $b 4 4 3 3 $colors.Warning
    Fill-Rect $b 8 4 3 3 $colors.Warning
    Fill-Rect $b 4 8 3 3 $colors.Warning
    Fill-Rect $b 8 8 3 3 $colors.Warning
}
New-Icon 'smart_distance' {
    param($b)
    Fill-Rect $b 7 2 1 12 $colors.Ice
    Fill-Rect $b 2 7 12 1 $colors.Ice
    Outline-Rect $b 5 5 6 6 $colors.Ice
    Fill-Rect $b 12 3 2 2 $colors.Warning
}
New-Icon 'portal_duration' {
    param($b)
    Outline-Rect $b 3 3 10 10 $colors.Ice
    Fill-Rect $b 7 5 2 4 $colors.Ice
    Fill-Rect $b 8 8 3 2 $colors.Ice
    Fill-Rect $b 6 1 4 2 $colors.Warning
}
New-Icon 'surface_range' {
    param($b)
    Fill-Rect $b 3 6 2 4 $colors.Warning
    Fill-Rect $b 7 4 2 8 $colors.Warning
    Fill-Rect $b 11 2 2 12 $colors.Warning
}
New-Icon 'entity_access' {
    param($b)
    Fill-Rect $b 3 2 4 4 $colors.Portal
    Fill-Rect $b 9 3 4 4 $colors.Portal
    Fill-Rect $b 2 9 5 4 $colors.Portal
    Fill-Rect $b 8 9 6 5 $colors.Portal
}

function Draw-Aperture($Bitmap, $PortalColor, $ArrowColor) {
    Outline-Rect $Bitmap 5 5 6 6 $PortalColor
    Fill-Rect $Bitmap 3 3 3 1 $ArrowColor
    Fill-Rect $Bitmap 3 3 1 3 $ArrowColor
    Fill-Rect $Bitmap 10 3 3 1 $ArrowColor
    Fill-Rect $Bitmap 12 3 1 3 $ArrowColor
    Fill-Rect $Bitmap 3 12 3 1 $ArrowColor
    Fill-Rect $Bitmap 3 10 1 3 $ArrowColor
    Fill-Rect $Bitmap 10 12 3 1 $ArrowColor
    Fill-Rect $Bitmap 12 10 1 3 $ArrowColor
}
New-Icon 'aperture_on'  { param($b) Draw-Aperture $b $colors.Portal $colors.Ice }
New-Icon 'aperture_off' { param($b) Draw-Aperture $b $colors.Muted $colors.Muted }

function Draw-Pig($Bitmap, $Color) {
    Fill-Rect $Bitmap 2 3 12 9 $Color
    Fill-Rect $Bitmap 1 2 3 3 $Color
    Fill-Rect $Bitmap 12 2 3 3 $Color
    $snout = [System.Drawing.Color]::FromArgb(255, 231, 166, 172)
    $nostril = [System.Drawing.Color]::FromArgb(255, 91, 57, 65)
    Fill-Rect $Bitmap 5 9 6 4 $snout
    Fill-Rect $Bitmap 6 10 1 2 $nostril
    Fill-Rect $Bitmap 9 10 1 2 $nostril
}
New-Icon 'passive_transit_on'  { param($b) Draw-Pig $b $colors.Passive }
New-Icon 'passive_transit_off' { param($b) Draw-Pig $b $colors.Muted }

function Draw-Zombie($Bitmap, $Color) {
    Fill-Rect $Bitmap 2 2 12 12 $Color
    $eye = [System.Drawing.Color]::FromArgb(255, 38, 58, 44)
    $mouth = [System.Drawing.Color]::FromArgb(255, 77, 51, 45)
    Fill-Rect $Bitmap 4 5 3 2 $eye
    Fill-Rect $Bitmap 10 5 3 2 $eye
    Fill-Rect $Bitmap 5 10 7 2 $mouth
}
New-Icon 'hostile_transit_on'  { param($b) Draw-Zombie $b $colors.Hostile }
New-Icon 'hostile_transit_off' { param($b) Draw-Zombie $b $colors.Muted }

function Draw-Dragon($Bitmap, $Color) {
    Fill-Rect $Bitmap 3 4 10 9 $Color
    Fill-Rect $Bitmap 1 2 4 5 $Color
    Fill-Rect $Bitmap 11 2 4 5 $Color
    $eye = [System.Drawing.Color]::FromArgb(255, 231, 210, 255)
    $mouth = [System.Drawing.Color]::FromArgb(255, 58, 40, 70)
    Fill-Rect $Bitmap 5 7 2 2 $eye
    Fill-Rect $Bitmap 10 7 2 2 $eye
    Fill-Rect $Bitmap 6 11 5 3 $mouth
}
New-Icon 'boss_transit_on'  { param($b) Draw-Dragon $b $colors.Boss }
New-Icon 'boss_transit_off' { param($b) Draw-Dragon $b $colors.Muted }

New-Icon 'visuals' {
    param($b)
    Fill-Rect $b 5 4 5 1 $colors.Ice
    Fill-Rect $b 3 6 9 3 $colors.Ice
    Fill-Rect $b 5 10 5 1 $colors.Ice
    Fill-Rect $b 6 6 3 3 $colors.PanelRaised
    Fill-Rect $b 7 7 1 1 $colors.Ice
}
New-Icon 'dropdown' {
    param($b)
    Fill-Rect $b 4 6 8 1 $colors.Ice
    Fill-Rect $b 5 7 6 1 $colors.Ice
    Fill-Rect $b 6 8 4 1 $colors.Ice
    Fill-Rect $b 7 9 2 1 $colors.Ice
}
New-Icon 'back' {
    param($b)
    Fill-Rect $b 3 8 10 1 $colors.Ice
    Fill-Rect $b 3 7 4 3 $colors.Ice
    Fill-Rect $b 4 6 2 5 $colors.Ice
}
New-Icon 'module_back' {
    param($b)
    Fill-Rect $b 4 7 8 2 $colors.Ice
    Fill-Rect $b 4 6 2 4 $colors.Ice
    Fill-Rect $b 5 5 2 6 $colors.Ice
}
function Draw-Reset($Bitmap, $Color) {
    Fill-Rect $Bitmap 6 4 5 1 $Color
    Fill-Rect $Bitmap 10 5 2 2 $Color
    Fill-Rect $Bitmap 11 6 1 4 $Color
    Fill-Rect $Bitmap 5 10 6 1 $Color
    Fill-Rect $Bitmap 4 8 2 2 $Color
    Fill-Rect $Bitmap 4 8 3 1 $Color
    Fill-Rect $Bitmap 5 7 1 1 $Color
}
New-Icon 'reset_on'  { param($b) Draw-Reset $b $colors.Ice }
New-Icon 'reset_off' { param($b) Draw-Reset $b $colors.Muted }
New-Icon 'swirl' {
    param($b)
    Fill-Rect $b 7 4 4 1 $colors.Ice
    Fill-Rect $b 5 5 3 1 $colors.Ice
    Fill-Rect $b 4 6 2 4 $colors.Ice
    Fill-Rect $b 5 10 5 1 $colors.Ice
    Fill-Rect $b 9 8 2 2 $colors.Ice
    Fill-Rect $b 7 7 3 1 $colors.Ice
    Fill-Rect $b 7 6 1 1 $colors.Ice
}

New-Icon 'group_expanded' {
    param($b)
    Fill-Rect $b 4 6 7 1 $colors.Ice
    Fill-Rect $b 5 7 5 1 $colors.Ice
    Fill-Rect $b 6 8 3 1 $colors.Ice
    Fill-Rect $b 7 9 1 1 $colors.Ice
}
New-Icon 'group_collapsed' {
    param($b)
    Fill-Rect $b 6 4 1 7 $colors.Ice
    Fill-Rect $b 7 5 1 5 $colors.Ice
    Fill-Rect $b 8 6 1 3 $colors.Ice
    Fill-Rect $b 9 7 1 1 $colors.Ice
}
New-Icon 'drag_handle' {
    param($b)
    foreach ($y in 4, 7, 10) { Fill-Rect $b 5 $y 5 1 $colors.Muted }
}
New-Icon 'destination_dot_on'  { param($b) Fill-Rect $b 7 7 2 2 $colors.Ice }
New-Icon 'destination_dot_off' { param($b) Fill-Rect $b 7 7 2 2 $colors.Muted }

function Draw-Star($Bitmap, $Color, [bool] $Filled) {
    Fill-Rect $Bitmap 7 4 1 7 $Color
    Fill-Rect $Bitmap 4 7 7 1 $Color
    foreach ($p in @(@(5,5),@(9,5),@(5,9),@(9,9))) { Fill-Rect $Bitmap $p[0] $p[1] 1 1 $Color }
    if ($Filled) { Fill-Rect $Bitmap 6 6 3 3 $Color }
}
New-Icon 'star_on'  { param($b) Draw-Star $b $colors.StarOn $true }
New-Icon 'star_off' { param($b) Draw-Star $b $colors.StarOff $false }
New-Icon 'delete' {
    param($b)
    for ($pixel = 0; $pixel -lt 7; $pixel++) {
        Fill-Rect $b (4 + $pixel) (4 + $pixel) 1 1 $colors.Danger
        Fill-Rect $b (10 - $pixel) (4 + $pixel) 1 1 $colors.Danger
    }
}
New-Icon 'edit' {
    param($b)
    for ($pixel = 0; $pixel -lt 6; $pixel++) {
        Fill-Rect $b (4 + $pixel) (9 - $pixel) 2 2 $colors.Ice
    }
    Fill-Rect $b 4 10 2 2 $colors.Warning
}

$referencePath = Join-Path $PSScriptRoot '..\docs\art\gui-icons-reference.png'
$referenceFiles = Get-ChildItem -LiteralPath $outputDirectory -File -Filter '*.png' | Sort-Object Name
$columns = 5
$cellWidth = 180
$cellHeight = 58
$rows = [Math]::Ceiling($referenceFiles.Count / [double] $columns)
$reference = [System.Drawing.Bitmap]::new($columns * $cellWidth, $rows * $cellHeight,
    [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($reference)
$graphics.Clear([System.Drawing.Color]::FromArgb(255, 27, 29, 34))
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$font = [System.Drawing.Font]::new('Consolas', 9, [System.Drawing.FontStyle]::Regular,
    [System.Drawing.GraphicsUnit]::Pixel)
$brush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 232, 235, 238))
try {
    for ($index = 0; $index -lt $referenceFiles.Count; $index++) {
        $column = $index % $columns
        $row = [Math]::Floor($index / $columns)
        $x = $column * $cellWidth
        $y = $row * $cellHeight
        $icon = [System.Drawing.Image]::FromFile($referenceFiles[$index].FullName)
        try { $graphics.DrawImage($icon, $x + 14, $y + 18, 16, 16) } finally { $icon.Dispose() }
        $graphics.DrawString($referenceFiles[$index].BaseName, $font, $brush, $x + 47, $y + 21)
    }
    $reference.Save($referencePath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $brush.Dispose()
    $font.Dispose()
    $graphics.Dispose()
    $reference.Dispose()
}
