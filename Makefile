.PHONY: help mongo-up mongo-down backend backend-test frontend frontend-install frontend-test e2e clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-18s\033[0m %s\n", $$1, $$2}'

mongo-up: ## Start MongoDB (and mongo-express) via docker compose
	docker compose up -d mongo

mongo-down: ## Stop and remove the local MongoDB container
	docker compose down

backend: ## Run the Spring Boot backend
	cd backend && ./mvnw spring-boot:run

backend-test: ## Run backend unit + integration tests
	cd backend && ./mvnw -q verify

frontend-install: ## Install frontend dependencies
	cd frontend && npm install

frontend: ## Run the frontend dev server
	cd frontend && npm run dev

frontend-test: ## Run frontend unit tests
	cd frontend && npm run test

e2e: ## Run Playwright end-to-end specs (frontend + backend must already be running)
	cd frontend && npx playwright test

clean: ## Remove build artifacts
	rm -rf backend/target frontend/dist frontend/node_modules
