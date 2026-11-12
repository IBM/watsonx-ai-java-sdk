---
id: index
title: Migration Guide
---

# Migration Guide

Version-by-version guides for migrating between SDK releases that contain breaking changes.

| Version | Changes |
|---------|---------|
| [0.22.0 → 0.30.0](./0.22.0-to-0.30.0) | Typed chat request/response hierarchy, `DeploymentChatRequest`, `ModelGatewayService` |
| [0.30.1 → 0.40.0](./0.30.1-to-0.40.0) | Model Gateway chat types renamed to `ModelGatewayChat*`, chat exceptions moved package, `ModerationException`, unmodifiable collections |
| [0.40.0 → 0.50.0](./0.40.0-to-0.50.0) | Jackson split into `watsonx-ai-jackson2`/`watsonx-ai-jackson3` binding modules, `ProjectService`, context and generated-text detection, `ContainerReference`, custom HTTP log handlers, `ProjectService` base class renamed to `ScopedService`, removed `repetitionPenalty`/`lengthPenalty`/`enableGenericKvp` |
| [0.50.0 → 0.60.0](./0.50.0-to-0.60.0) | Binding-specific `WatsonxJacksonModule` packages, JSON provider class-loader fallback, explicit diagnostics for conflicting bindings |
