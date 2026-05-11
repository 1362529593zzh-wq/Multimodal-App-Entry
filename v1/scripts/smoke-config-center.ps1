param(
    [string]$BaseUrl = "http://127.0.0.1:8080"
)

$ErrorActionPreference = "Stop"

$checks = @(
    @{ Name = "Ping"; Url = "$BaseUrl/api/ping" },
    @{ Name = "Health"; Url = "$BaseUrl/actuator/health" },
    @{ Name = "Functions"; Url = "$BaseUrl/api/config/functions?pageNum=1&pageSize=5" },
    @{ Name = "ModelServices"; Url = "$BaseUrl/api/config/model-services?pageNum=1&pageSize=5" },
    @{ Name = "FunctionBindings"; Url = "$BaseUrl/api/config/function-model-bindings?pageNum=1&pageSize=5" },
    @{ Name = "ParamTemplates"; Url = "$BaseUrl/api/config/param-templates?pageNum=1&pageSize=5" }
)

$results = foreach ($check in $checks) {
    try {
        $response = Invoke-RestMethod -Uri $check.Url -Method Get
        [pscustomobject]@{
            Name = $check.Name
            Url = $check.Url
            Ok = $true
            Code = $response.code
            Message = $response.message
        }
    }
    catch {
        [pscustomobject]@{
            Name = $check.Name
            Url = $check.Url
            Ok = $false
            Code = $null
            Message = $_.Exception.Message
        }
    }
}

$results | Format-Table -AutoSize

if ($results.Ok -contains $false) {
    throw "One or more smoke checks failed."
}
