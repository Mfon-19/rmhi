variable "region" {
  type    = string
  default = "us-east-1"
}

variable "project" {
  type    = string
  default = "eureka-scraper"
}

variable "alert_email" {
  type = string
}

variable "google_api_key" {
  type        = string
  description = "The API key for Google Gemini."
  sensitive   = true
}

variable "backend_firebase_secret_arn" {
  type        = string
  description = "Secrets Manager ARN holding Firebase service account JSON for the backend."
}

variable "backend_cors_allowed_origins" {
  type        = string
  description = "Comma-separated list of allowed CORS origins for the backend."
  default     = "http://localhost:3000"
}

variable "backend_desired_count" {
  type    = number
  default = 1
}

variable "backend_cpu" {
  type    = number
  default = 512
}

variable "backend_memory" {
  type    = number
  default = 1024
}

variable "backend_image_tag" {
  type    = string
  default = "latest"
}
