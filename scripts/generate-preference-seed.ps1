$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$books = @(Import-Csv (Join-Path $root "db\books.csv"))
$users = @(Import-Csv (Join-Path $root "db\users.csv") | Where-Object login_id -like "user*")
$tierTemplates = @(Import-Csv (Join-Path $root "db\tier-templates.csv"))
$idealTemplates = @(Import-Csv (Join-Path $root "db\ideal-templates.csv"))
$ratings = @(Import-Csv (Join-Path $root "db\ratings.csv"))

function Get-UnitRandom([long]$A, [long]$B = 0, [long]$C = 0, [long]$D = 0) {
    $modulus = 2147483647L
    $value = ($A * 73856093L + $B * 19349663L + $C * 83492791L + $D * 2654435761L) % $modulus
    if ($value -lt 0) { $value += $modulus }
    return [double]$value / [double]$modulus
}

$ratingStats = @{}
foreach ($group in ($ratings | Group-Object book_id)) {
    $average = [double](($group.Group | Measure-Object score -Average).Average)
    $ratingStats[[int]$group.Name] = @{
        Average = $average
        Count = $group.Count
        Popularity = [Math]::Max(0.0, [Math]::Min(1.0,
                (($average - 2.2) / 2.75) * 0.76 + (($group.Count - 20) / 80.0) * 0.24))
    }
}

$tierTemplates[0].title = "해리 포터 시리즈"
$tierTemplates[0].description = "해리 포터 7부작을 한눈에 비교하는 시리즈 티어리스트"
$tierTemplates | Export-Csv (Join-Path $root "db\tier-templates.csv") -NoTypeInformation -Encoding utf8NoBOM

function Select-Books([object[]]$Candidates, [int]$Count, [int]$Salt) {
    $selected = @($Candidates | Sort-Object { ([int]$_.book_id * $Salt) % 1009 }, { [int]$_.book_id } | Select-Object -First $Count)
    if ($selected.Count -lt $Count) { throw "템플릿 도서 부족: 필요 $Count, 실제 $($selected.Count)" }
    return $selected
}

$tierRules = @{
    1 = @{ Books = @($books | Where-Object author -eq "조앤 케이 롤링"); Count = 7; Salt = 3 }
    2 = @{ Books = @($books | Where-Object genre -eq "SF"); Count = 24; Salt = 5 }
    3 = @{ Books = @($books | Where-Object genre -eq "판타지"); Count = 24; Salt = 7 }
    4 = @{ Books = @($books | Where-Object genre -eq "추리"); Count = 24; Salt = 11 }
    5 = @{ Books = $books; Count = 24; Salt = 13 }
    6 = @{ Books = $books; Count = 24; Salt = 17 }
    7 = @{ Books = @($books | Where-Object author -like "히가시노 게이고*"); Count = 24; Salt = 19 }
    8 = @{ Books = @($books | Where-Object author -like "박경리*"); Count = 20; Salt = 23 }
}

$tierTemplateItems = [System.Collections.Generic.List[object]]::new()
foreach ($template in $tierTemplates) {
    $id = [int]$template.template_id
    $rule = $tierRules[$id]
    $selected = Select-Books $rule.Books $rule.Count $rule.Salt
    for ($index = 0; $index -lt $selected.Count; $index++) {
        $tierTemplateItems.Add([pscustomobject]@{template_id=$id;book_id=$selected[$index].book_id;sort_order=$index})
    }
}
$tierTemplateItems | Export-Csv (Join-Path $root "db\tier-template-items.csv") -NoTypeInformation -Encoding utf8NoBOM

$idealRules = @{
    1 = @{ Books = $books; Salt = 29 }
    2 = @{ Books = @($books | Where-Object genre -eq "판타지"); Salt = 31 }
    3 = @{ Books = @($books | Where-Object genre -eq "SF"); Salt = 37 }
    4 = @{ Books = @($books | Where-Object genre -eq "추리"); Salt = 41 }
    5 = @{ Books = @($books | Where-Object genre -eq "디스토피아"); Salt = 43 }
    6 = @{ Books = $books; Salt = 47 }
    7 = @{ Books = $books; Salt = 53 }
    8 = @{ Books = $books; Salt = 59 }
}

$idealTemplateItems = [System.Collections.Generic.List[object]]::new()
$idealBooksByTemplate = @{}
foreach ($template in $idealTemplates) {
    $id = [int]$template.template_id
    $rule = $idealRules[$id]
    $selected = @(Select-Books $rule.Books 16 $rule.Salt)
    $idealBooksByTemplate[$id] = $selected
    for ($index = 0; $index -lt $selected.Count; $index++) {
        $idealTemplateItems.Add([pscustomobject]@{template_id=$id;book_id=$selected[$index].book_id;sort_order=$index})
    }
}
$idealTemplateItems | Export-Csv (Join-Path $root "db\ideal-template-items.csv") -NoTypeInformation -Encoding utf8NoBOM

$tierResults = [System.Collections.Generic.List[object]]::new()
$tierResultItems = [System.Collections.Generic.List[object]]::new()
$listId = 0
foreach ($user in $users) {
    $userNumber = [int]($user.login_id -replace "user", "")
    foreach ($template in $tierTemplates) {
        if (($userNumber * 31 + [int]$template.template_id * 17) % 10 -ge 7) { continue }
        $listId++
        $templateId = [int]$template.template_id
        $tierResults.Add([pscustomobject]@{tier_list_id=$listId;login_id=$user.login_id;template_id=$templateId;title="$($user.nickname)의 $($template.title)";description=""})
        $items = @($tierTemplateItems | Where-Object {[int]$_.template_id -eq $templateId})

        $rankedItems = @($items | ForEach-Object {
            $bookId = [int]$_.book_id
            $popularity = [double]$ratingStats[$bookId].Popularity
            $personalTaste = Get-UnitRandom $userNumber $bookId $templateId 11
            $opinionNoise = Get-UnitRandom $userNumber $bookId $templateId 23
            [pscustomobject]@{
                Item = $_
                Score = $popularity * 0.68 + $personalTaste * 0.24 + $opinionNoise * 0.08
            }
        } | Sort-Object Score -Descending)

        $itemCount = $rankedItems.Count
        $sCount = [Math]::Max(1, [Math]::Round($itemCount * (0.12 + (Get-UnitRandom $userNumber $templateId 31) * 0.08)))
        $aCount = [Math]::Max(1, [Math]::Round($itemCount * (0.20 + (Get-UnitRandom $userNumber $templateId 37) * 0.08)))
        $bCount = [Math]::Max(1, [Math]::Round($itemCount * (0.22 + (Get-UnitRandom $userNumber $templateId 41) * 0.08)))
        $cCount = [Math]::Max(1, [Math]::Round($itemCount * (0.17 + (Get-UnitRandom $userNumber $templateId 43) * 0.08)))
        if ($sCount + $aCount + $bCount + $cCount -ge $itemCount) {
            $cCount = [Math]::Max(1, $itemCount - $sCount - $aCount - $bCount - 1)
        }

        for ($rank = 0; $rank -lt $rankedItems.Count; $rank++) {
            $grade = if ($rank -lt $sCount) { "S" }
                elseif ($rank -lt $sCount + $aCount) { "A" }
                elseif ($rank -lt $sCount + $aCount + $bCount) { "B" }
                elseif ($rank -lt $sCount + $aCount + $bCount + $cCount) { "C" }
                else { "D" }
            $tierResultItems.Add([pscustomobject]@{
                tier_list_id = $listId
                book_id = $rankedItems[$rank].Item.book_id
                tier_grade = $grade
                sort_order = $rank
            })
        }
    }
}
$tierResults | Export-Csv (Join-Path $root "db\tier-results.csv") -NoTypeInformation -Encoding utf8NoBOM
$tierResultItems | Export-Csv (Join-Path $root "db\tier-result-items.csv") -NoTypeInformation -Encoding utf8NoBOM

$idealResults = [System.Collections.Generic.List[object]]::new()
$idealMatches = [System.Collections.Generic.List[object]]::new()
$runId = 0
foreach ($user in $users) {
    $userNumber = [int]($user.login_id -replace "user", "")
    foreach ($template in $idealTemplates) {
        if (($userNumber * 37 + [int]$template.template_id * 19) % 10 -ge 6) { continue }
        $runId++
        $templateId = [int]$template.template_id
        $source = @($idealBooksByTemplate[$templateId])
        $roundBooks = @($source | Sort-Object {
            Get-UnitRandom $userNumber ([int]$_.book_id) $templateId 59
        })
        $allMatches = [System.Collections.Generic.List[object]]::new()
        $roundSize = 16
        while ($roundSize -ge 2) {
            $winners = [System.Collections.Generic.List[object]]::new()
            for ($matchOrder = 0; $matchOrder -lt $roundBooks.Count / 2; $matchOrder++) {
                $left = $roundBooks[$matchOrder * 2]
                $right = $roundBooks[$matchOrder * 2 + 1]
                $leftId = [int]$left.book_id
                $rightId = [int]$right.book_id
                $leftTaste = (Get-UnitRandom $userNumber $leftId $templateId 67) - 0.5
                $rightTaste = (Get-UnitRandom $userNumber $rightId $templateId 67) - 0.5
                $leftStrength = [double]$ratingStats[$leftId].Popularity * 0.76 + $leftTaste * 0.24
                $rightStrength = [double]$ratingStats[$rightId].Popularity * 0.76 + $rightTaste * 0.24
                $leftProbability = 1.0 / (1.0 + [Math]::Exp(-7.5 * ($leftStrength - $rightStrength)))
                $choice = Get-UnitRandom $userNumber $templateId $roundSize ($matchOrder * 131 + $leftId + $rightId)
                $winner = if ($choice -lt $leftProbability) { $left } else { $right }
                $allMatches.Add([pscustomobject]@{run_id=$runId;round_size=$roundSize;match_order=$matchOrder;left_book_id=$left.book_id;right_book_id=$right.book_id;winner_book_id=$winner.book_id})
                $winners.Add($winner)
            }
            $roundBooks = @($winners)
            $roundSize = [int]($roundSize / 2)
        }
        $idealResults.Add([pscustomobject]@{run_id=$runId;template_id=$templateId;login_id=$user.login_id;bracket_size=16;winner_book_id=$roundBooks[0].book_id})
        foreach ($match in $allMatches) { $idealMatches.Add($match) }
    }
}
$idealResults | Export-Csv (Join-Path $root "db\ideal-results.csv") -NoTypeInformation -Encoding utf8NoBOM
$idealMatches | Export-Csv (Join-Path $root "db\ideal-result-matches.csv") -NoTypeInformation -Encoding utf8NoBOM

Write-Host "템플릿 항목과 사용자 참여 결과 CSV 재생성 완료"
