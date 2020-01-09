# API-Extractor-UsageCounter
Extract all API (types, methods, fields, constructors) from source code, and then count their frequencies by parsing client code

## Configurations:
1. **java/extractor/Main.java/67** : path to jar file of target library
2. **java/extractor/Main.java/183** : path to source code of target library
3. **java/apiUsageCounter/ApiUsageCounter.java/101** : path to jar file of target library
4. **java/apiUsageCounter/ApiUsageCounter.java/205** : path to client code of target library

## Usage
1. run **java/extractor/Main.java** to extract all API from source code
2. run **java/apiUsageCounter/ApiUsageCounter.java** to parse client code, and get frequencies of the extracted API
3. find results in **data** folder

