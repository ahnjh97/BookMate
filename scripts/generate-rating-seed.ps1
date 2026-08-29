param(
    [string]$OutputPath = (Join-Path $PSScriptRoot "..\db\ratings.csv")
)

$observationsByScore = @{
    1 = @("기대했던 방향과 달라 아쉬움이 컸어요.", "끝까지 읽기가 조금 버거웠어요.", "내용에 쉽게 몰입하기 어려웠어요.", "소재에 비해 전개가 아쉬웠어요.", "저와는 잘 맞지 않는 책이었어요.", "기억에 남는 부분이 많지 않았어요.", "설명과 실제 내용의 인상이 달랐어요.", "읽는 흐름이 자주 끊기는 느낌이었어요.")
    2 = @("흥미로운 부분도 있었지만 아쉬움이 남아요.", "초반에 비해 후반부가 조금 약했어요.", "소재는 좋았지만 전개가 잘 맞지는 않았어요.", "몇몇 부분은 인상적이었어요.", "기대보다는 평범하게 느껴졌어요.", "장점과 단점이 분명한 책이었어요.", "내용이 조금 더 간결했으면 좋았을 것 같아요.", "집중되는 부분과 그렇지 않은 부분이 나뉘었어요.")
    3 = @("부담 없이 무난하게 읽었어요.", "흥미로운 부분과 아쉬운 부분이 함께 있었어요.", "전반적으로 괜찮은 독서 경험이었어요.", "기대했던 정도로 재미있게 읽었어요.", "한 번쯤 읽어 볼 만한 책이에요.", "몇몇 대목이 기억에 남았어요.", "취향에 따라 평가가 달라질 것 같아요.", "전체적으로 균형이 잘 잡힌 편이에요.")
    4 = @("생각보다 몰입해서 읽었어요.", "내용의 흐름이 자연스럽고 좋았어요.", "읽고 나서도 기억에 남는 부분이 많아요.", "주제와 구성이 잘 어우러졌어요.", "기대 이상으로 만족스럽게 읽었어요.", "천천히 다시 읽어 보고 싶은 책이에요.", "책장을 넘기는 시간이 즐거웠어요.", "공감되는 대목이 많아 인상 깊었어요.")
    5 = @("처음부터 끝까지 깊이 몰입해서 읽었어요.", "오래 기억에 남을 만큼 인상 깊었어요.", "최근 읽은 책 중 가장 만족스러웠어요.", "읽는 내내 기대를 뛰어넘는 책이었어요.", "제 취향에 정말 잘 맞는 책이었어요.", "다 읽고 바로 다시 펼쳐 보고 싶었어요.", "내용과 구성이 모두 훌륭했어요.", "마지막 장을 덮은 뒤에도 여운이 길게 남았어요.")
}

$closingsByScore = @{
    1 = @("다시 읽을 것 같지는 않아요.", "다른 작품을 먼저 권하고 싶어요.", "조금 더 다듬어졌다면 좋았을 것 같아요.", "아쉽지만 완독한 데 의미를 두려고 해요.")
    2 = @("취향이 맞는 사람에게는 괜찮을 수도 있어요.", "조금 아쉽지만 시도 자체는 좋았어요.", "다음 작품은 더 기대해 보려고 해요.", "인상적인 부분만큼 아쉬운 부분도 있었어요.")
    3 = @("가볍게 읽을 책을 찾는다면 괜찮아요.", "시간이 지나면 다시 생각날 수도 있을 것 같아요.", "큰 기대 없이 읽으면 무난하게 즐길 수 있어요.", "주변의 평가도 한번 들어 보고 싶어요.")
    4 = @("비슷한 취향의 사람에게 추천하고 싶어요.", "다른 작품도 찾아 읽어 보고 싶어요.", "나중에 한 번 더 읽어 볼 생각이에요.", "읽을지 고민한다면 권해 주고 싶어요.")
    5 = @("주변 사람에게 꼭 추천하고 싶어요.", "소장해 두고 다시 읽고 싶은 책이에요.", "다른 사람의 감상도 궁금해지는 작품이에요.", "망설이지 않고 추천할 수 있어요.")
}

$rows = [System.Collections.Generic.List[object]]::new()
for ($bookId = 1; $bookId -le 1000; $bookId++) {
    $ratingCount = 20 + (($bookId * 47 + [math]::Floor($bookId / 11) * 23) % 81)
    $qualitySeed = (($bookId * 73 + [math]::Floor($bookId / 9) * 41) % 1000) / 1000
    $bookScore = 2.7 + 2.1 * [math]::Sqrt($qualitySeed)
    if ($bookId % 23 -eq 0) { $bookScore = 2.2 + 0.9 * $qualitySeed }
    if ($bookId % 29 -eq 0) { $bookScore = 4.65 + 0.25 * $qualitySeed }

    for ($offset = 0; $offset -lt $ratingCount; $offset++) {
        $userNumber = (($bookId * 29 + $offset * 37) % 100) + 1
        $userBias = ((($userNumber * 29) % 11) - 5) * 0.06
        $noiseA = (($bookId * 97 + $userNumber * 53 + $offset * 31) % 101) / 100
        $noiseB = (($bookId * 43 + $userNumber * 71 + $offset * 17) % 101) / 100
        $rawScore = $bookScore + $userBias + (($noiseA + $noiseB - 1) * 0.95)
        $score = [math]::Max(1, [math]::Min(5, [math]::Round($rawScore, 0, [MidpointRounding]::AwayFromZero)))

        $observations = $observationsByScore[[int]$score]
        $closings = $closingsByScore[[int]$score]
        $observation = $observations[($bookId * 7 + $userNumber * 11) % $observations.Count]
        $closing = $closings[($bookId * 13 + $userNumber * 17) % $closings.Count]
        $comment = if (($bookId + $userNumber) % 5 -eq 0) { $observation } else { "$observation $closing" }

        $rows.Add([pscustomobject]@{
            login_id = "user$userNumber"
            book_id = $bookId
            score = $score
            comment_text = $comment
        })
    }
}

$rows | Export-Csv -Path $OutputPath -NoTypeInformation -Encoding utf8NoBOM
Write-Host "평점 및 후기 $($rows.Count.ToString('N0'))개 생성됨: $OutputPath"
