param(
    [string]$OutputPath = "docs/localization-catalog.md"
)

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$englishPath = Join-Path $repositoryRoot "src/main/resources/assets/riftgun/lang/en_us.json"
$chinesePath = Join-Path $repositoryRoot "src/main/resources/assets/riftgun/lang/zh_cn.json"
$resolvedOutput = Join-Path $repositoryRoot $OutputPath

$english = Get-Content -LiteralPath $englishPath -Raw | ConvertFrom-Json -AsHashtable
$chinese = Get-Content -LiteralPath $chinesePath -Raw | ConvertFrom-Json -AsHashtable

if ($english.Count -ne $chinese.Count) {
    throw "Language key counts differ: en_us=$($english.Count), zh_cn=$($chinese.Count)"
}

$missingChinese = @($english.Keys | Where-Object { -not $chinese.ContainsKey($_) })
$missingEnglish = @($chinese.Keys | Where-Object { -not $english.ContainsKey($_) })
if ($missingChinese.Count -gt 0 -or $missingEnglish.Count -gt 0) {
    throw "Language key sets differ"
}

function ConvertTo-MarkdownCell([string]$text) {
    return $text.Replace("|", "\|").Replace("`r`n", "<br>").Replace("`n", "<br>")
}

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("# RiftGun 双语文本完整目录")
$lines.Add("")
$lines.Add("来源：``en_us.json`` 与 ``zh_cn.json``。共 $($english.Count) 项；按 key 排序，不省略任何本地化文本。")
$lines.Add("")

$groups = $english.Keys | Sort-Object | Group-Object { ($_ -split '\.')[0] }
foreach ($group in $groups) {
    $lines.Add("## $($group.Name)")
    $lines.Add("")
    $lines.Add("| Key | English | 中文 |")
    $lines.Add("|---|---|---|")
    foreach ($key in $group.Group) {
        $en = ConvertTo-MarkdownCell ([string]$english[$key])
        $zh = ConvertTo-MarkdownCell ([string]$chinese[$key])
        $lines.Add("| ``$key`` | $en | $zh |")
    }
    $lines.Add("")
}

$outputDirectory = Split-Path -Parent $resolvedOutput
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
[System.IO.File]::WriteAllLines($resolvedOutput, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Output "Wrote $($english.Count) entries to $resolvedOutput"
