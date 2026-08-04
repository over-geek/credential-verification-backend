---
name: spring-boot-layered-architecture
description: Enforces clean, modular package structure for Java/Spring Boot backend code — model, repository, service, service.impl, and controller packages, with thin controllers, all business logic in services, and DTOs at the REST boundary. Use this whenever building, reviewing, or refactoring a Spring Boot (or similar layered Java) backend, whenever the user asks for "clean code," "modular structure," "separation of concerns," or mentions REST APIs, controllers, services, or repositories in Java — even if they don't explicitly ask for an architecture pattern by name.
---

# Spring Boot Layered Architecture

A convention for structuring Java/Spring Boot backend code into clear, single-responsibility
layers so business logic stays out of controllers and persistence stays out of services.
Apply this whenever writing or reviewing backend Java code, regardless of the specific
domain (this skill is domain-agnostic — it applies the same way to a credential system,
an e-commerce backend, or anything else).

## Package structure

Organize backend code into these packages:

- **`model`** — entities/domain objects (e.g. JPA `@Entity` classes). No business logic.
  Just field definitions, relationships, and basic annotations.
- **`repository`** — data access only (e.g. Spring Data JPA repository interfaces, or
  custom query classes). No business logic — just persistence operations (save, find,
  delete, custom queries).
- **`service`** — interfaces defining business operations (e.g. `WidgetService` with
  methods like `createWidget`, `getById`, `updateStatus`). This is the contract, decoupled
  from its implementation, so callers depend on behavior, not concrete classes.
- **`service.impl`** (or `serviceimpl`) — concrete implementations of the service
  interfaces. **All core business logic lives here**: validation, orchestration across
  multiple repositories, calls to other services, mapping between entities and DTOs, and
  any conditional/branching logic that isn't pure persistence.
- **`controller`** — REST endpoints only. Controllers must be **thin**.

## Thin controllers — the core rule

A controller method should read as a simple linear sequence:

```
receive DTO → call service → return DTO
```

If a controller method contains conditionals, loops, direct repository calls, or entity
manipulation, that logic belongs in a service instead. A good litmus test: could you read
every controller method in under 5 seconds and know exactly what it delegates to? If not,
logic has leaked into the controller and should be moved down to `service.impl`.

Controllers should not:
- Contain `if`/`else` branching beyond basic request validation delegation
- Call repositories directly
- Construct or manipulate entities
- Contain loops over collections for business purposes (mapping a list is fine if it's
  trivial DTO conversion, but non-trivial iteration logic belongs in a service)

## DTOs at the REST boundary

- Never return or accept JPA entities directly in controller method signatures — always
  use DTOs for request and response payloads.
- Keep entity ↔ DTO mapping inside the service layer (in `service.impl`), or in a small
  dedicated mapper class/package (e.g. `mapper`) if mapping logic grows large enough to
  warrant its own home. Do not put mapping logic in controllers.
- Typical DTO split for a resource:
  - A **request DTO** containing only the fields a client is allowed to submit (excludes
    server-generated fields like IDs, timestamps, or computed values).
  - A **response DTO** containing the full shape returned to clients (may include IDs,
    computed fields, or related data flattened in).
- Naming convention: `{Resource}RequestDto`, `{Resource}ResponseDto` (adjust to match
  the project's existing conventions if one is already established).

## Example skeleton

For a resource called `Widget`:

```
model/
  Widget.java                    // @Entity, fields only

repository/
  WidgetRepository.java           // extends JpaRepository<Widget, UUID>

service/
  WidgetService.java               // interface: createWidget(), getById(), ...

service/impl/
  WidgetServiceImpl.java           // implements WidgetService, all logic + mapping

controller/
  WidgetController.java            // @RestController, delegates to WidgetService

dto/
  WidgetRequestDto.java
  WidgetResponseDto.java
```

`WidgetController` should look roughly like:

```java
@RestController
@RequestMapping("/widgets")
public class WidgetController {

    private final WidgetService widgetService;

    public WidgetController(WidgetService widgetService) {
        this.widgetService = widgetService;
    }

    @PostMapping
    public WidgetResponseDto create(@RequestBody WidgetRequestDto request) {
        return widgetService.createWidget(request);
    }

    @GetMapping("/{id}")
    public WidgetResponseDto getById(@PathVariable UUID id) {
        return widgetService.getById(id);
    }
}
```

Note the controller never touches `Widget` (the entity) or `WidgetRepository` directly —
only DTOs and the service interface.

## When a project has additional constraints

If a project's own instructions (e.g. an AGENTS.md, README, or CONTRIBUTING doc) specify
a different package layout or additional layers (e.g. a separate `mapper` or `validator`
package), defer to those project-specific instructions for naming and additional
structure, but keep applying the core principles here: thin controllers, business logic
in services, no entities crossing the REST boundary.

## Scope note

This skill governs code organization and layering, not deployment topology, security,
testing strategy, or database schema design — those are separate concerns to be handled
per the project's own requirements.
