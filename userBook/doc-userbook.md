Tester Userbook manuellement
==============================

1. Déclencher l'alimentation (si pas déjà fait) : http://localhost:8005/admin
2. Tester l'annuaire/ma classe :
    - Page standard de l'annuaire : http://localhost:8101/annuaire
    - Page Ma Classe : http://localhost:8101/annuaire?query=class=CM2%20de%20Mme%20Rousseau
    - Rechercher des personnes : pour l'instant, bête regexp sur les 3 premiers chars (pour l'instant personne n'a de centres d'intérêts)
3. Initialiser un compte : http://localhost:8101/mon-compte?init=Laure%20BOURG (simulation d'une première authent)
4. Récupérer l'id de retour et tester Mon Compte avec cet id : http://localhost:8101/mon-compte?id={id} (avec n'importe quel autre id, pas de centres d'intérêts)
6. Modifier des centres d'intérêts : enregistrement sur blur() (le requête ne finit pas, mais au rechargement les modifs sont là / idem pour la visibilité)
7. Modifier la visibilité d'une catégorie (la mise à jour du statut de visibilité courant ne fonctionne pas)
