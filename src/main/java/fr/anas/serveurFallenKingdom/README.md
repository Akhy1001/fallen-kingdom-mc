# Plugin Fallen Kingdom (Paper 1.21.x)

Plugin Minecraft complet implémentant le mode de jeu **Fallen Kingdom (FK)** pour serveur **Paper 1.21.x**[span_1](start_span)[span_1](end_span). Conçu avec une architecture modulaire orientée objet, une gestion complète des états de partie, un système de capture d'autel par présence et une persistance automatique en YAML[span_2](start_span)[span_2](end_span)[span_3](start_span)[span_3](end_span)[span_4](start_span)[span_4](end_span).

---

## Fonctionnalités principales

* **Gestion des équipes :** Création, suppression, couleurs personnalisées (préfixe dans le chat, la TabList et au-dessus de la tête), répartition aléatoire équilibrée, chat d'équipe privé (`!message`) et gestion du Friendly Fire[span_5](start_span)[span_5](end_span)[span_6](start_span)[span_6](end_span).
* **Bases et protection :** Délimitation des bases par baguette de sélection (2 coins) ou par rayon autour de l'administrateur[span_7](start_span)[span_7](end_span)[span_8](start_span)[span_8](end_span). Blocage de la pose/casse hors de sa base (sauf liste blanche configurable) et protection totale des bases ennemies avant le jour des assauts[span_9](start_span)[span_9](end_span)[span_10](start_span)[span_10](end_span). Notifications d'entrée et de sortie de base[span_11](start_span)[span_11](end_span).
* **Cycle de jeu automatisé :** Horloge interne gérant le défilement des jours (20 min par défaut) et les déblocages automatiques avec annonces visuelles (Titres) et sonores : PvP (J3), Nether (J4), Assauts/TNT (J5) et End (J7)[span_12](start_span)[span_12](end_span)[span_13](start_span)[span_13](end_span)[span_14](start_span)[span_14](end_span)[span_15](start_span)[span_15](end_span). Support de la mise en pause et reprise du timer[span_16](start_span)[span_16](end_span)[span_17](start_span)[span_17](end_span).
* **Autels et conditions de victoire :** Autel indestructible placé dans chaque base[span_18](start_span)[span_18](end_span)[span_19](start_span)[span_19](end_span). Capture par présence ennemie avec barre de progression (**BossBar**), alerte sonore/visuelle aux défenseurs et annulation immédiate si un défenseur entre dans la zone[span_20](start_span)[span_20](end_span)[span_21](start_span)[span_21](end_span). Passage en spectateur des joueurs morts sans autel et déclaration automatique du vainqueur avec récapitulatif des statistiques (Kills, Morts, Captures)[span_22](start_span)[span_22](end_span)[span_23](start_span)[span_23](end_span).
* **Scoreboard et Persistance YAML :** Scoreboard latéral mis à jour en temps réel[span_24](start_span)[span_24](end_span). Sauvegarde automatique asynchrone (`teams.yml` et `game.yml`) permettant de reprendre la partie exactement au même jour et à la même seconde après un redémarrage du serveur[span_25](start_span)[span_25](end_span)[span_26](start_span)[span_26](end_span)[span_27](start_span)[span_27](end_span)[span_28](start_span)[span_28](end_span).

---

## Prérequis et Installation

### Prérequis
* **Java :** JDK 21 ou supérieur
* **Serveur :** Paper 1.21.x (testé sur Paper 1.21.1)[span_29](start_span)[span_29](end_span)[span_30](start_span)[span_30](end_span)
* **Build :** Gradle[span_31](start_span)[span_31](end_span)[span_32](start_span)[span_32](end_span)

### Compilation depuis les sources
```bash
./gradlew jar
```

### Installation sur un serveur
1. Récupérez le fichier `.jar` compilé dans `build/libs/`[span_33](start_span)[span_33](end_span).
2. Déposez-le dans le dossier `plugins/` de votre serveur Paper 1.21.x[span_34](start_span)[span_34](end_span).
3. Démarrez le serveur : les fichiers de configuration (`config.yml`, `messages.yml`) seront générés automatiquement dans `plugins/ServeurFallenKingdom/`[span_35](start_span)[span_35](end_span)[span_36](start_span)[span_36](end_span)[span_37](start_span)[span_37](end_span).

---

## Déroulement d'une partie type (Guide rapide)

1. **Créer les équipes :**
   ```text
   /fk team create Rouge RED
   /fk team create Bleu BLUE
   ```
2. **Définir les bases :**
    * Soit avec un rayon autour de vous : `/fk base set Rouge 15`[span_38](start_span)[span_38](end_span)[span_39](start_span)[span_39](end_span)
    * Soit avec la baguette (`/fk base wand`), clic gauche (coin 1) + clic droit (coin 2), puis `/fk base set Rouge`[span_40](start_span)[span_40](end_span)[span_41](start_span)[span_41](end_span)[span_42](start_span)[span_42](end_span).
3. **Placer les autels :**
   Regardez le bloc au centre de chaque base et exécutez :
   ```text
   /fk altar set Rouge
   /fk altar set Bleu
   ```
4. **Répartir les joueurs :**
   Les joueurs rejoignent avec `/fk join <équipe>`, ou l'admin utilise `/fk team random`[span_43](start_span)[span_43](end_span).
5. **Lancer la partie :**
   ```text
   /fk start
   ```
   Tous les joueurs sont téléportés dans leur base respective et le Jour 1 commence automatiquement[span_44](start_span)[span_44](end_span)[span_45](start_span)[span_45](end_span).

---

## Commandes et Permissions

Toutes les commandes disposent de l'auto-complétion via la touche `Tab`[span_46](start_span)[span_46](end_span).

### Commandes Joueurs (`fk.player` — accordée par défaut)
| Commande | Description |
| :--- | :--- |
| `/fk info` | Affiche l'état de la partie, le jour, le timer, les déblocages et les équipes[span_47](start_span)[span_47](end_span). |
| `/fk join <équipe>` | Permet de rejoindre une équipe avant le début de la partie (Lobby)[span_48](start_span)[span_48](end_span). |
| `/fk team chat <message>` | Envoie un message privé aux membres de son équipe (ou préfixe `!` dans le chat)[span_49](start_span)[span_49](end_span). |

### Commandes Administrateur (`fk.admin` — opérateur par défaut)
| Commande | Description |
| :--- | :--- |
| `/fk team create <nom> <couleur>` | Crée une nouvelle équipe avec la couleur spécifiée (`RED`, `BLUE`, `GREEN`, `YELLOW`...)[span_50](start_span)[span_50](end_span). |
| `/fk team delete <nom>` | Supprime une équipe existante[span_51](start_span)[span_51](end_span). |
| `/fk team add <joueur> <équipe>` | Force l'affectation d'un joueur dans une équipe[span_52](start_span)[span_52](end_span). |
| `/fk team random` | Répartit équitablement tous les joueurs sans équipe[span_53](start_span)[span_53](end_span)[span_54](start_span)[span_54](end_span). |
| `/fk base wand` | Donne la baguette de sélection de zone de base[span_55](start_span)[span_55](end_span). |
| `/fk base set <équipe> [rayon]` | Définit la zone de base et le point de spawn d'une équipe[span_56](start_span)[span_56](end_span)[span_57](start_span)[span_57](end_span). |
| `/fk altar set <équipe>` | Définit le bloc visé comme autel indestructible de l'équipe[span_58](start_span)[span_58](end_span)[span_59](start_span)[span_59](end_span). |
| `/fk start` | Lance la partie, téléporte les équipes et démarre le Jour 1[span_60](start_span)[span_60](end_span)[span_61](start_span)[span_61](end_span). |
| `/fk pause` / `/fk resume` | Met la partie en pause (gèle le timer et les actions) ou la reprend[span_62](start_span)[span_62](end_span)[span_63](start_span)[span_63](end_span). |
| `/fk stop` | Arrête la partie en cours et réinitialise le cycle au Jour 1[span_64](start_span)[span_64](end_span). |
| `/fk day <n>` | Force immédiatement le passage au jour `n` (utile pour les tests)[span_65](start_span)[span_65](end_span). |
| `/fk reload` | Recharge les fichiers `config.yml` et `messages.yml` (hors partie en cours)[span_66](start_span)[span_66](end_span)[span_67](start_span)[span_67](end_span). |

---

## Configuration (`config.yml`)

Le fichier `config.yml` permet de paramétrer l'intégralité des règles sans modifier le code[span_68](start_span)[span_68](end_span)[span_69](start_span)[span_69](end_span) :

```yaml
game:
  day-duration-minutes: 20    # Durée d'une journée en minutes
  max-teams: 4                # Nombre max d'équipes (0 = illimité)
  max-players-per-team: 5     # Joueurs max par équipe
  friendly-fire: false        # Autoriser ou non les dégâts entre alliés

unlocks:                      # Jours de déblocage des phases
  pvp: 3
  nether: 4
  assault: 5
  end: 7

altar:
  block: BEACON               # Type de bloc utilisé pour l'autel
  capture-seconds: 30         # Temps requis pour capturer un autel ennemi
  capture-radius: 3           # Rayon de détection autour de l'autel (en blocs)

build:
  allowed-outside-base:       # Blocs autorisés hors de sa base
    - TORCH
    - LADDER
    - TNT                     # Actif uniquement à partir du jour d'assaut

scoreboard:
  enabled: true
  title: "&6&lFallen Kingdom"
```

---

## Architecture du projet

```text
src/main/java/fr/anas/serveurFallenKingdom/
├── ServeurFallenKingdom.java      # Point d'entrée (onEnable / onDisable)
├── command/
│   └── FkCommand.java             # Gestionnaire de commandes /fk et TabCompleter
├── config/
│   ├── ConfigService.java         # Lecture et validation de config.yml
│   └── Messages.java              # Chargement des textes traduisibles (messages.yml)
├── game/
│   ├── GameState.java             # Énumération des états (WAITING, STARTING, RUNNING, PAUSED, ENDED)
│   ├── GameManager.java           # Cycle de vie de la partie, victoires et statistiques
│   └── DayScheduler.java          # Timer des journées et annonces de déblocages
├── team/
│   ├── Team.java                  # Modèle de données d'une équipe
│   └── TeamManager.java           # Gestion des équipes et synchronisation TabList
├── base/
│   ├── Region.java                # Cuboïde 3D représentant une zone
│   ├── Base.java                  # Association équipe, région et point de spawn
│   └── BaseManager.java           # Gestion des bases et baguette de sélection
├── altar/
│   ├── Altar.java                 # Modèle de l'autel et BossBar de progression
│   ├── AltarManager.java          # Registre des autels
│   └── CaptureTask.java           # Tâche périodique de détection et capture d'autel
├── listener/
│   ├── BlockListener.java         # Protection des bases, autels et liste blanche
│   ├── PvpListener.java           # Régulation du PvP et du Friendly Fire
│   ├── PortalListener.java        # Verrouillage des portails Nether et End
│   ├── DeathListener.java         # Respawn en base, passage en spectateur et éliminations
│   └── ChatListener.java          # Formatage du chat global et canal d'équipe (!)
├── scoreboard/
│   └── ScoreboardService.java     # Affichage dynamique du panneau latéral
└── storage/
    └── YamlStorage.java           # Sauvegarde asynchrone et restauration (teams.yml, game.yml)
```
