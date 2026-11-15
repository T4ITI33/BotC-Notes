# BotC Notes

Mod Fabric (client) de bloc-notes pour les parties de Blood on the Clocktower. Minecraft 1.21.4.

## Compiler

Il faut Java 21.

```
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

Le jar se trouve dans `build/libs/`.

## Code

- `src/main/java/fr/botc/notes/BotcNotes.java` : point d'entrée, touches N / Alt
- `NoteScreen.java` : écran du bloc-notes
- `NoteRenderer.java` : affichage
- `NoteData.java` : sauvegarde des notes
- `src/main/resources/assets/botc-notes/lang/` : textes FR / EN
