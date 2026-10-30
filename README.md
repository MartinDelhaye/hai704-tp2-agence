# HAI704I TP 2 - Projet `agence`

> Francisco-Ernesto Suarez-Roca.
> 
> Martin Delhaye

Ce projet est le **consommateur** : il appelle le service web des hôtels.
Il **ne dépend d'aucune classe du projet `hotel`** : tout ce qu'il connaît vient du contrat (WSDL + XSD).

> État : A4

## 1. Rôle et relations

| Projet | Rôle | Lien |
|---|---|---|
| `hotel` | fournisseur : publie le service SOAP | l'agence en lit le contrat (`?wsdl`, `?xsd=1`), jamais le code |
| `agence` | client : interroge l'hôtel | classes engendrées par `wsimport` |

## 2. Environnement

- **JDK** : Eclipse Temurin 17.0.20.1 (compilation avec `--release 17`)
- **Maven** : 3.9.x
- **IDE** : IntelliJ IDEA 2026.2.3 (Windows). Les commandes ci-dessous sont des commandes Maven, indépendantes de l'IDE.

## 3. Dépendances

| Dépendance | Version | Usage |
|---|---|---|
| `com.sun.xml.ws:jaxws-rt` | 4.0.5 | exécution du client SOAP (Eclipse Metro) |
| `com.sun.xml.ws:jaxws-maven-plugin` | 4.0.5 | exécute `wsimport` pour engendrer le client |
| `org.codehaus.mojo:exec-maven-plugin` | 3.1.0 | lancement avec `mvn exec:java` |

## 4. Où et comment le client est engendré

Le contrat est **copié dans le projet** pour que la compilation ne nécessite pas que l'hôtel soit lancé :

```
src/main/resources/wsdl/
├── hotel.wsdl
└── hotel.xsd
```

Ces deux fichiers ont été récupérés depuis un hôtel en marche :

```
curl.exe -s "http://localhost:8081/hotel?wsdl"  -o src/main/resources/wsdl/hotel.wsdl
curl.exe -s "http://localhost:8081/hotel?xsd=1" -o src/main/resources/wsdl/hotel.xsd
```

**Modification manuelle** : dans `hotel.wsdl`, l'attribut `schemaLocation` (qui pointait vers `http://localhost:8081/hotel?xsd=1`) a été remplacé par `hotel.xsd`.
Si le contrat de l'hôtel change, il faut récupérer à nouveau ces fichiers.

`wsimport` (via `jaxws-maven-plugin`, phase `generate-sources`) produit les classes dans :

```
target/generated-sources/wsimport/hai704i/tp2/agence/generated/
```

## 5. Construction et exécution

Démarrer d'abord au moins un hôtel (voir le README de `hotel`), puis depuis la racine de `agence` :

```
mvn -q clean compile
mvn -q exec:java "-Dexec.args=http://localhost:8081/hotel AG-01"
```

Arguments de `AgenceMain` :

| Position | Signification | Valeur par défaut |
|---|---|---|
| 1 | adresse du point d'accès de l'hôtel | `http://localhost:8081/hotel` |
| 2 | identifiant de l'agence | `AG-01` |

L'adresse n'est pas figée dans le code : la changer ne demande aucune recompilation.
Dans IntelliJ : *Run > Edit Configurations*, champ **Program arguments** (trois configurations sont utilisées : « Agence OK », « Agence port erroné », « Agence inconnue »).
