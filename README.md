# 📡 ClassCord — Client de messagerie réseau local (BTS SIO SLAM)

## 👤 Auteur
**Nom** : [Ton nom ici]  
**Classe** : BTS SIO SLAM — Promo 2024  

## 📌 Présentation

ClassCord est un client de messagerie instantanée développé en Java avec Swing dans le cadre d'une semaine intensive SLAM.  
L'application permet aux utilisateurs d'échanger en réseau local, via un serveur TCP, des messages globaux et privés et de gérer leurs statuts.

Le projet respecte une architecture **MVC** et utilise les sockets Java pour la communication réseau en JSON.

---

## 📚 Fonctionnalités

- Connexion à un serveur distant (IP + port saisis au lancement)
- Connexion en mode invité ou authentifié (optionnel)
- Affichage des messages globaux et privés dans l'interface
- Liste des utilisateurs connectés avec icône de statut :
  - 🟢 en ligne
  - 🟠 absent
  - 🔴 occupé
  - ⚪ invisible
- Envoi de messages privés à un utilisateur sélectionné
- Changement de statut personnel via un menu déroulant
- Rafraîchissement automatique de la liste des utilisateurs et statuts toutes les 3 secondes
- Interface fluide et responsive avec Swing et threads réseau séparés

---

## ⚙️ Installation et exécution

### Prérequis :
- Java 11+
- Maven
- Serveur ClassCord (fourni par le formateur ou un camarade SISR)

### Installation :

```bash
git clone https://github.com/votre-identifiant/classcord-client.git
cd classcord-client
mvn compile
