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

