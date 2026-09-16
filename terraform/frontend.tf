# Frontend se sirve desde nginx en la EC2 (AWS Academy no permite CloudFront/OAC).
# HTTPS real con Let's Encrypt + sslip.io sobre la Elastic IP.

output "frontend_url" {
  description = "URL HTTPS del frontend (nginx en EC2)."
  value       = "https://${replace(aws_eip.backend.public_ip, ".", "-")}.sslip.io"
}

output "frontend_http_url" {
  description = "URL HTTP (solo bootstrap certbot)."
  value       = "http://${aws_eip.backend.public_ip}"
}

output "artifacts_note" {
  value = "Los jars se suben por SCP (scripts/deploy-ec2.sh); Academy limita S3/CloudFront."
}
