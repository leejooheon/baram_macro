package common.network


// OCR 서버가 다른 PC에 있으면 OCR_HOST 환경변수로 바꿀 수 있다
val host: String = System.getenv("OCR_HOST") ?: "192.168.0.2"
const val ocrPort = 5001
const val commanderPort = 5002
