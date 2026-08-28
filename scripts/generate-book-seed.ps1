param(
    [int]$Count = 1000,
    [string]$OutputPath = (Join-Path $PSScriptRoot "..\db\books.csv")
)

$ErrorActionPreference = "Stop"

$genres = [ordered]@{
    "소설" = @{ Quota = 140; Queries = @("소설 베스트셀러", "한국 소설", "영미 소설", "일본 소설", "장편 소설", "단편 소설") }
    "판타지" = @{ Quota = 88; Queries = @("판타지 소설 베스트셀러", "판타지 소설", "한국 판타지", "마법 소설", "모험 소설") }
    "SF" = @{ Quota = 75; Queries = @("SF 소설 베스트셀러", "SF 소설", "과학 소설", "우주 소설") }
    "추리" = @{ Quota = 95; Queries = @("추리 소설 베스트셀러", "추리 소설", "미스터리 소설", "탐정 소설") }
    "스릴러" = @{ Quota = 70; Queries = @("스릴러 소설 베스트셀러", "스릴러 소설", "범죄 소설", "심리 스릴러") }
    "디스토피아" = @{ Quota = 25; Queries = @("디스토피아 소설", "미래 사회 소설", "미래 소설", "사회 소설", "재난 소설") }
    "로맨스" = @{ Quota = 75; Queries = @("로맨스 소설 베스트셀러", "로맨스 소설", "사랑 소설", "한국 로맨스") }
    "역사소설" = @{ Quota = 65; Queries = @("역사 소설 베스트셀러", "역사 소설", "시대 소설", "한국 역사 소설") }
    "고전" = @{ Quota = 90; Queries = @("세계 고전 문학", "한국 고전 문학", "고전 소설", "세계문학") }
    "역사" = @{ Quota = 55; Queries = @("역사 교양 베스트셀러", "역사 교양", "한국사", "세계사") }
    "과학" = @{ Quota = 60; Queries = @("과학 교양 베스트셀러", "과학 교양", "물리학", "생명 과학") }
    "전기" = @{ Quota = 40; Queries = @("인물 평전 베스트셀러", "인물 평전", "자서전", "인물 전기") }
    "자기계발" = @{ Quota = 25; Queries = @("자기계발 베스트셀러", "습관 베스트셀러", "성장 에세이") }
    "IT" = @{ Quota = 45; Queries = @("프로그래밍 베스트셀러", "프로그래밍", "소프트웨어 개발", "인공지능 교양") }
    "철학" = @{ Quota = 45; Queries = @("철학 교양 베스트셀러", "철학 교양", "동양 철학", "서양 철학") }
}

function Has-Hangul([string]$Value) {
    return -not [string]::IsNullOrWhiteSpace($Value) -and $Value -match "[가-힣]"
}

function Is-ExcludedBook([string]$Title, [string]$Description) {
    if ($Title -match '(?i)(세트|SET|합본|전집|박스본|한정판|특별판|리커버)') { return $true }
    if ($Title -match '(?i)(수험|기출|문제집|워크북|교재|자격증|시험|모의고사|요약집)') { return $true }
    if ($Title -match '(?:\s|\()(?:상|중|하)(?:권)?\)?$') { return $true }
    if ($Title -match '(?:전\s*)?\d+권(?:\s*완결)?$') { return $true }
    if ($Title -match '\s\d{1,3}$') {
        $numberedStandalone = @('화씨 451', '1984', '1Q84', '82년생 김지영', '제5도살장', '7년의 밤')
        $numberedSeriesPrefixes = @('신 퇴마록 ', '퇴마록 ', '룬의 아이들 ', '살인자의 쇼핑몰 ')
        $isKnownSeries = $numberedSeriesPrefixes | Where-Object { $Title.StartsWith($_) }
        if ($numberedStandalone -notcontains $Title -and -not $isKnownSeries) { return $true }
    }
    return $false
}

function Get-ShortDescription([string]$Description) {
    $maxLength = 120
    if ($Description.Length -le $maxLength) {
        return $Description
    }

    $sentenceMatch = [regex]::Match($Description, '^.{20,119}?[.!?](?=\s|$)')
    if ($sentenceMatch.Success) {
        return $sentenceMatch.Value.Trim()
    }

    $shortened = $Description.Substring(0, $maxLength - 1)
    $lastSpace = $shortened.LastIndexOf(' ')
    if ($lastSpace -ge 60) {
        $shortened = $shortened.Substring(0, $lastSpace)
    }
    return "$($shortened.TrimEnd(' ', '.', ',', '!', '?'))…"
}

$rows = [System.Collections.Generic.List[object]]::new()
$seen = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
$requiredBooks = @(
    @{ title="해리 포터와 마법사의 돌"; date="2025-01-15"; publisher="문학수첩"; image="https://contents.kyobobook.co.kr/sih/fit-in/600x0/pdt/9791193790656.jpg"; description="해리가 호그와트에 입학해 마법 세계와 자신의 운명을 처음 마주하는 이야기." },
    @{ title="해리 포터와 비밀의 방"; date="2025-01-17"; publisher="문학수첩"; image="https://contents.kyobobook.co.kr/sih/fit-in/600x0/pdt/9791193790663.jpg"; description="호그와트의 비밀의 방이 열리며 해리와 친구들이 학교를 위협하는 존재를 추적하는 이야기." },
    @{ title="해리 포터와 아즈카반의 죄수"; date="2025-02-12"; publisher="문학수첩"; image="https://contents.kyobobook.co.kr/sih/fit-in/600x0/pdt/9791193790670.jpg"; description="아즈카반을 탈옥한 시리우스 블랙과 해리의 과거에 얽힌 진실이 밝혀지는 이야기." },
    @{ title="해리 포터와 불의 잔"; date="2025-10-14"; publisher="문학수첩"; image="https://contents.kyobobook.co.kr/sih/fit-in/600x0/pdt/9791193790960.jpg"; description="트라이위저드 대회에 뜻밖에 참가한 해리가 위험한 과제와 어둠의 귀환에 맞서는 이야기." },
    @{ title="해리 포터와 불사조 기사단"; date="2022-10-31"; publisher="문학수첩"; image="https://contents.kyobobook.co.kr/sih/fit-in/600x0/pdt/9788983921956.jpg"; description="마법 정부의 억압 속에서 해리와 친구들이 덤블도어의 군대를 조직해 저항하는 이야기." },
    @{ title="해리 포터와 혼혈 왕자"; date="2024-11-28"; publisher="포터모어"; image="https://contents.kyobobook.co.kr/sih/fit-in/600x0/pdt/450D000235800.jpg"; description="덤블도어와 해리가 볼드모트의 과거와 호크룩스의 비밀을 파헤치는 이야기." },
    @{ title="해리 포터와 죽음의 성물"; date="2024-11-28"; publisher="포터모어"; image="https://contents.kyobobook.co.kr/sih/fit-in/600x0/pdt/450D000235802.jpg"; description="해리와 친구들이 마지막 호크룩스를 찾아 나서며 볼드모트와 최후의 전투를 벌이는 이야기." }
)
foreach ($book in $requiredBooks) {
    $rows.Add([pscustomobject]@{
        book_id = $rows.Count + 1
        author = "조앤 케이 롤링"
        title = $book.title
        genre = "판타지"
        publisher = $book.publisher
        published_date = $book.date
        description = $book.description
        image_url = $book.image
    })
    $null = $seen.Add("$($book.title)|조앤 케이 롤링")
}
foreach ($entry in $genres.GetEnumerator()) {
    $quota = [int]$entry.Value.Quota
    $genreRows = [System.Collections.Generic.List[object]]::new()

    foreach ($query in $entry.Value.Queries) {
        for ($page = 1; $page -le 40 -and $genreRows.Count -lt $quota; $page++) {
            $keyword = [uri]::EscapeDataString($query)
            $uri = "https://www.kyobobook.co.kr/api/gw/aco/search/commodity?keyword=$keyword&gbCode=TOT&page=$page"
            $response = Invoke-RestMethod -Uri $uri -Headers @{
                "Accept" = "application/json"
                "User-Agent" = "Mozilla/5.0 BookMateSeedBuilder/1.0"
            }
            $booksWithDate = @($response.data.resultDocuments | ForEach-Object -Parallel {
                $book = $_
                try {
                    $detailUri = "https://product.kyobobook.co.kr/api/gw/pdt/product/$($book.sale_CMDTID)"
                    $detail = Invoke-RestMethod -Uri $detailUri -Headers @{
                        "Accept" = "application/json"
                        "User-Agent" = "Mozilla/5.0 BookMateSeedBuilder/1.0"
                    }
                    $value = [string]$detail.data.top.info.publisher.pubDate
                    if ($value -match "^(\d{4})(\d{2})(\d{2})$") {
                        $book | Add-Member -NotePropertyName published_date -NotePropertyValue (
                            "$($Matches[1])-$($Matches[2])-$($Matches[3])"
                        ) -Force
                        $book
                    }
                } catch {
                    return
                }
            } -ThrottleLimit 12)

            foreach ($book in $booksWithDate) {
                if ($genreRows.Count -ge $quota) { break }
                if ($book.sale_CMDT_DVSN_CODE -ne "KOR" -or $book.sale_CMDT_GRP_DVSN_CODE -ne "SGK") { continue }
                $title = ([string]$book.cmdt_NAME).Trim()
                $author = ([string]$book.chrc_NAME).Trim()
                $publisher = ([string]$book.pbcm_NAME).Trim()
                $imageUrl = [string]$book.img_URL
                if (-not (Has-Hangul $title) -or -not (Has-Hangul $author)) { continue }
                if ($title -match "[A-Za-z]" -or $author -match "[A-Za-z]") { continue }
                if (-not (Has-Hangul $publisher) -or [string]::IsNullOrWhiteSpace($imageUrl)) { continue }
                if ($title.Length -gt 200 -or $author.Length -gt 100 -or $publisher.Length -gt 100) { continue }
                $publishedDate = [string]$book.published_date
                if ([string]::IsNullOrWhiteSpace($publishedDate)) { continue }

                $key = "$title|$author"
                if (-not $seen.Add($key)) { continue }
                $description = [regex]::Replace([string]$book.annt_CTTN, "<[^>]+>", " ")
                $description = ([regex]::Replace($description, "\s+", " ")).Trim()
                if (-not (Has-Hangul $description)) {
                    $description = "$($entry.Key) 분야의 한국어판 도서"
                }
                if (Is-ExcludedBook $title $description) { continue }
                $description = Get-ShortDescription $description
                $imageUrl = $imageUrl.Replace("http://", "https://").Replace("/fit-in/200x0/", "/fit-in/600x0/")

                $genreRows.Add([pscustomobject]@{
                    book_id = $rows.Count + $genreRows.Count + 1
                    author = $author
                    title = $title
                    genre = $entry.Key
                    publisher = $publisher
                    published_date = $publishedDate
                    description = $description
                    image_url = $imageUrl
                })
            }
        }
    }

    if ($genreRows.Count -lt $quota) {
        throw "$($entry.Key) 도서를 $quota 권 수집하지 못했습니다. 수집 결과: $($genreRows.Count)권"
    }
    foreach ($row in $genreRows) { $rows.Add($row) }
}

if ($rows.Count -ne $Count) {
    throw "요청한 $Count 권과 수집 결과가 다릅니다: $($rows.Count)권"
}

$rows | Export-Csv -LiteralPath $OutputPath -NoTypeInformation -Encoding utf8NoBOM
Write-Host "한국어 도서 CSV 생성 완료: $($rows.Count)권"
