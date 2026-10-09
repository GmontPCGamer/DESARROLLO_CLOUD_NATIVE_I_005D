# EC2 dedicada a RabbitMQ. Los microservicios de la EC2 de aplicación
# se conectan por la IP privada (puerto 5672). La consola (15672) queda
# pública solo para la demo del lab.

resource "random_password" "rabbitmq" {
  length  = 20
  special = false
}

resource "aws_security_group" "rabbitmq" {
  name        = "${var.nombre_proyecto}-rabbitmq-sg"
  description = "RabbitMQ: AMQP desde la app y consola de la demo"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "AMQP desde la EC2 de la aplicacion"
    from_port       = 5672
    to_port         = 5672
    protocol        = "tcp"
    security_groups = [aws_security_group.nexotech.id]
  }

  ingress {
    description     = "AMQP nodo 2 desde la EC2 de la aplicacion"
    from_port       = 5673
    to_port         = 5673
    protocol        = "tcp"
    security_groups = [aws_security_group.nexotech.id]
  }

  ingress {
    description = "Consola nodo 1 (demo)"
    from_port   = 15672
    to_port     = 15672
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Consola nodo 2 (demo)"
    from_port   = 15673
    to_port     = 15673
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
    Name = "${var.nombre_proyecto}-rabbitmq-sg"
  }
}

resource "aws_instance" "rabbitmq" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = "t3.small"
  subnet_id                   = sort(data.aws_subnets.default.ids)[0]
  vpc_security_group_ids      = [aws_security_group.rabbitmq.id]
  key_name                    = var.ec2_key_name
  iam_instance_profile        = var.ec2_instance_profile
  associate_public_ip_address = true

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  user_data = <<-EOF
    #!/bin/bash
    set -eux
    dnf install -y docker
    systemctl enable --now docker
    mkdir -p /usr/libexec/docker/cli-plugins /opt/nexotech-rabbit
    curl -fsSL -o /usr/libexec/docker/cli-plugins/docker-compose \
      https://github.com/docker/compose/releases/download/v2.32.4/docker-compose-linux-x86_64
    chmod +x /usr/libexec/docker/cli-plugins/docker-compose
    echo "Cluster: copiar rabbitmq/docker-compose.yml y rabbitmq.conf a /opt/nexotech-rabbit y ejecutar docker compose up -d"
  EOF

  lifecycle {
    ignore_changes = [user_data, ami]
  }

  tags = {
    Name = "${var.nombre_proyecto}-rabbitmq"
  }
}

output "rabbitmq_instance_id" {
  value = aws_instance.rabbitmq.id
}

output "rabbitmq_private_ip" {
  description = "IP que usan los microservicios (SPRING_RABBITMQ_HOST)."
  value       = aws_instance.rabbitmq.private_ip
}

output "rabbitmq_public_ip" {
  description = "IP de la consola de RabbitMQ."
  value       = aws_instance.rabbitmq.public_ip
}

output "rabbitmq_console" {
  value = "http://${aws_instance.rabbitmq.public_ip}:15672"
}

output "rabbitmq_console_node2" {
  value = "http://${aws_instance.rabbitmq.public_ip}:15673"
}

output "rabbitmq_user" {
  value = "nexotech"
}

output "rabbitmq_password" {
  value     = random_password.rabbitmq.result
  sensitive = true
}
