# Local kind workflow for the allocator. Run from this folder: `make up`, `make help`.
CLUSTER   := call-cluster
CONTEXT   := kind-$(CLUSTER)
NAMESPACE := call-allocation
IMAGE     := call-allocation-service:v1.0.0
ROOT      := ..
KUBECTL   := kubectl --context $(CONTEXT)

.DEFAULT_GOAL := help
.PHONY: help cluster-up cluster-down build load deploy undeploy restart status logs port-forward seed up down

help: ## List targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "} {printf "  %-14s %s\n", $$1, $$2}'

cluster-up: ## Create the kind cluster (no-op if it already exists)
	@kind get clusters | grep -qx $(CLUSTER) || kind create cluster --name $(CLUSTER) --config k8s/cluster.yaml

cluster-down: ## Delete the kind cluster
	kind delete cluster --name $(CLUSTER)

build: ## Build the allocator image into the local docker daemon
	cd $(ROOT) && ./gradlew :app:dockerBuild

load: build ## Load the image into the kind nodes
	kind load docker-image $(IMAGE) --name $(CLUSTER)

deploy: ## Apply the allocator manifests and wait for the rollout
	$(KUBECTL) apply -k k8s/allocator
	$(KUBECTL) -n $(NAMESPACE) rollout status deployment/allocator --timeout=120s

undeploy: ## Remove the allocator manifests
	$(KUBECTL) delete -k k8s/allocator --ignore-not-found

restart: ## Restart the allocator so it picks up a freshly loaded image
	$(KUBECTL) -n $(NAMESPACE) rollout restart deployment/allocator
	$(KUBECTL) -n $(NAMESPACE) rollout status deployment/allocator --timeout=120s

status: ## Show nodes and allocator resources
	$(KUBECTL) get nodes
	$(KUBECTL) -n $(NAMESPACE) get deploy,pods,svc -o wide

logs: ## Follow allocator logs
	$(KUBECTL) -n $(NAMESPACE) logs -f deployment/allocator

port-forward: ## Expose the allocator on localhost:8080
	$(KUBECTL) -n $(NAMESPACE) port-forward svc/allocator 8080:80

seed: ## Run the seed script against localhost:8080 (needs port-forward running)
	$(ROOT)/scripts/seed-and-report.sh

up: cluster-up load deploy ## Create cluster, build and load image, deploy

down: cluster-down ## Tear everything down
