.PHONY: help mongo-up mongo-down download ingest ingest-dry backend backend-test frontend frontend-install frontend-test e2e test clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-18s\033[0m %s\n", $$1, $$2}'

mongo-up: ## Start MongoDB (and mongo-express) via docker compose
	docker compose up -d mongo

mongo-down: ## Stop and remove the local MongoDB container
	docker compose down

download: ## Download the raw MISeD JSONL files into data/raw/
	./tools/download_mised.sh

ingest: ## Transform the raw JSONL into Mongo collections (drops existing data first)
	python3 tools/ingest/ingest_mised.py --dir data/raw --uri "$${MONGODB_URI:-mongodb://localhost:27017}" --db diagna --drop \
		--emit-fixtures backend/src/test/resources/fixtures/

ingest-dry: ## Dry-run the ingest transform without writing to Mongo
	python3 tools/ingest/ingest_mised.py --dir data/raw --dry-run

test: ## Run the Python ETL unit tests
	cd tools/ingest && python3 -m pytest tests -q

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
