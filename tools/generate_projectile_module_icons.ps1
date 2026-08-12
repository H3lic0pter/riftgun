Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$itemDir = Join-Path $root "src/main/resources/assets/riftgun/textures/item/modules"
$guiDir = Join-Path $root "src/main/resources/assets/riftgun/textures/gui/sprites/icons"

function New-PixelIcon([string]$path, [scriptblock]$draw) {
    $bitmap = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.Color]::Transparent)
    & $draw $graphics
    $graphics.Dispose()
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function Brush([string]$hex) {
    return [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($hex))
}

New-PixelIcon (Join-Path $itemDir "matter_anchor_module.png") {
    param($g)
    $dark = Brush "#343348"; $metal = Brush "#77758E"; $light = Brush "#B7B3D0"; $core = Brush "#5BD6C2"
    $g.FillRectangle($dark, 3, 1, 10, 14); $g.FillRectangle($metal, 4, 2, 8, 12)
    $g.FillRectangle($light, 5, 3, 6, 2); $g.FillRectangle($dark, 6, 5, 4, 6)
    $g.FillRectangle($core, 7, 6, 2, 4); $g.FillRectangle($dark, 1, 7, 4, 3)
    $g.FillRectangle($dark, 11, 7, 4, 3); $g.FillRectangle($light, 6, 12, 4, 2)
    $dark.Dispose(); $metal.Dispose(); $light.Dispose(); $core.Dispose()
}

New-PixelIcon (Join-Path $itemDir "projectile_transit_module.png") {
    param($g)
    $dark = Brush "#3D4050"; $gold = Brush "#D7C65C"; $light = Brush "#FFF0A0"; $rift = Brush "#54C7C0"
    $g.FillRectangle($dark, 2, 2, 12, 12); $g.FillRectangle($rift, 4, 4, 8, 8)
    $g.FillRectangle($dark, 5, 5, 6, 6); $g.FillRectangle($gold, 1, 7, 11, 2)
    $g.FillRectangle($light, 10, 5, 2, 6); $g.FillRectangle($gold, 12, 6, 3, 4)
    $dark.Dispose(); $gold.Dispose(); $light.Dispose(); $rift.Dispose()
}

function Entity-FallGuard([string]$path, [string]$bodyColor, [string]$groundColor) {
    New-PixelIcon $path {
        param($g)
        $body = Brush $bodyColor; $ground = Brush $groundColor
        $g.FillRectangle($body, 6, 1, 4, 4); $g.FillRectangle($body, 5, 5, 6, 6)
        $g.FillRectangle($body, 3, 6, 2, 3); $g.FillRectangle($body, 11, 6, 2, 3)
        $g.FillRectangle($body, 5, 11, 2, 3); $g.FillRectangle($body, 9, 11, 2, 3)
        $g.FillRectangle($ground, 2, 14, 12, 1); $g.FillRectangle($ground, 4, 15, 8, 1)
        $body.Dispose(); $ground.Dispose()
    }
}

Entity-FallGuard (Join-Path $guiDir "entity_fall_guard_on.png") "#9CD7A4" "#4E8E63"
Entity-FallGuard (Join-Path $guiDir "entity_fall_guard_off.png") "#858991" "#55585F"

function Projectile-Transit([string]$path, [string]$arrowColor, [string]$riftColor) {
    New-PixelIcon $path {
        param($g)
        $arrow = Brush $arrowColor; $rift = Brush $riftColor; $dark = Brush "#3E4148"
        $g.FillRectangle($rift, 9, 2, 2, 12); $g.FillRectangle($dark, 8, 4, 1, 8)
        $g.FillRectangle($arrow, 1, 7, 11, 2); $g.FillRectangle($arrow, 10, 5, 2, 6)
        $g.FillRectangle($arrow, 12, 6, 3, 4); $g.FillRectangle($dark, 2, 5, 1, 2)
        $g.FillRectangle($dark, 2, 10, 1, 2)
        $arrow.Dispose(); $rift.Dispose(); $dark.Dispose()
    }
}

Projectile-Transit (Join-Path $guiDir "projectile_transit_on.png") "#F1DD76" "#61D4C9"
Projectile-Transit (Join-Path $guiDir "projectile_transit_off.png") "#858991" "#55585F"

