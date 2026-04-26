group "default" {
  targets = ["backend", "frontend"]
}

target "backend" {
  context    = "./backend"
  dockerfile = "Dockerfile"
  tags       = ["qalam/backend:latest"]
}

target "frontend" {
  context    = "./frontend"
  dockerfile = "Dockerfile"
  target     = "deps"
  tags       = ["qalam/frontend:latest"]
}
