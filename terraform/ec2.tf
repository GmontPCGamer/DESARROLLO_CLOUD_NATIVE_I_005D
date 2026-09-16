# =============================================================================
# EC2: BFF + 8 microservicios (demo 48h, H2 en memoria)
# AWS Academy: LabInstanceProfile + key vockey
# =============================================================================

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*-x86_64"]
  }

  filter {
    name   = "state"
    values = ["available"]
  }
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

resource "aws_security_group" "nexotech" {
  name        = "${var.nombre_proyecto}-sg"
  description = "NexoTech demo: SSH + BFF 8080"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP (nginx + certbot)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS (SPA nginx)"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "BFF (API Gateway y health)"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.nombre_proyecto}-sg"
  }
}

resource "aws_instance" "backend" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.ec2_instance_type
  subnet_id                   = sort(data.aws_subnets.default.ids)[0]
  vpc_security_group_ids      = [aws_security_group.nexotech.id]
  key_name                    = var.ec2_key_name
  iam_instance_profile        = var.ec2_instance_profile
  associate_public_ip_address = true

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  user_data = <<-EOF
    #!/bin/bash
    set -eux
    dnf update -y
    dnf install -y java-17-amazon-corretto-headless awscli unzip
    mkdir -p /opt/nexotech/jars /opt/nexotech/logs /opt/nexotech/bin
    cat >/etc/profile.d/nexotech.sh <<'ENV'
    export SPRING_PROFILES_ACTIVE=azuread
    export AZURE_TENANT_ID=${var.azure_tenant_id}
    export AZURE_API_AUDIENCE=${var.audiencia}
    export APP_ADMIN_USERS=${var.app_admin_users}
    export JAVA_TOOL_OPTIONS="-Xms64m -Xmx192m"
    ENV
  EOF

  tags = {
    Name = "${var.nombre_proyecto}-backend"
  }
}

resource "aws_eip" "backend" {
  domain   = "vpc"
  instance = aws_instance.backend.id

  tags = {
    Name = "${var.nombre_proyecto}-eip"
  }
}

output "ec2_public_ip" {
  description = "IP pública (Elastic IP) del backend."
  value       = aws_eip.backend.public_ip
}

output "ec2_instance_id" {
  value = aws_instance.backend.id
}

output "ec2_ssh" {
  value = "ssh -i ~/.ssh/vockey.pem ec2-user@${aws_eip.backend.public_ip}"
}
