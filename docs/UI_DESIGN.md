# PHANToM VPhone Soft Minimal UI

## Direction

The v0.4.3-alpha interface uses a soft-minimal dashboard rather than the original
VirtualApp utility look.

Design goals:
- Calm and lightweight rather than visually technical.
- Clear hierarchy with fewer competing controls.
- Soft rounded surfaces without heavy shadows.
- Neutral off-white background with muted indigo and mint accents.
- Keep all critical runtime controls discoverable without exposing engine details.

## Home

The Home screen now contains:
- PHANToM VPhone title and a short status chip.
- Add app quick action.
- Camera Provider quick action with the active provider visible at a glance.
- Guest Apps section with a visible app count.
- Three-column Guest card grid for more breathing room than the old four-column grid.
- A visible overflow button on each Guest card while preserving long-press actions.
- A redesigned empty state.

## Add App

The app picker uses the same off-white background and rounded list cards.
The install action is a compact rounded indigo button rather than a full-width
legacy Android control.

## Settings

Settings uses a light, transparent toolbar and the same background language as Home.

## Interaction

- Tap Guest card: launch.
- Tap Guest overflow: management menu.
- Long-press Guest card: management menu remains available.
- Tap Add app: open the app importer/picker.
- Tap Camera card: choose Camera Provider.
- Long-press Camera card: run Camera Test.

## Location behavior

The UI does not expose VirtualApp synthetic-location controls.
Guests use Android device-location passthrough. If Android is currently supplying
location through a selected Mock Location app, Guests receive that Android-provided
location as well.
