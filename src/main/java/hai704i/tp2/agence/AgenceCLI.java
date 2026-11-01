package hai704i.tp2.agence;

import hai704i.tp2.agence.AgenceService.*;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;

public class AgenceCLI {

    private static class FinDeSaisie extends RuntimeException {
    }

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final AgenceService agence;
    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private List<OffreAgence> offres = List.of();

    public AgenceCLI(AgenceService agence) {
        this.agence = agence;
    }


    public void lancer() {
        System.out.println("=== Agence " + agence.identifiant() + " ===");
        try {
            boolean continuer = true;
            while (continuer) {
                System.out.println("\n1. Rechercher un séjour\n2. Revoir les offres\n3. Réserver une offre\n0. Quitter");
                try {
                    switch (entier("Votre choix : ", 0, 3)) {
                        case 1 -> rechercher();
                        case 2 -> consulter();
                        case 3 -> reserver();
                        default -> continuer = false;
                    }
                } catch (FinDeSaisie e) {
                    throw e;
                } catch (RuntimeException e) {
                    System.out.println("  ! Erreur inattendue (" + e.getClass().getSimpleName() + "). Retour au menu.");
                }
            }
        } catch (FinDeSaisie e) {
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Interrompu.");
        }
        System.out.println("Au revoir.");
    }

    private void rechercher() throws InterruptedException {
        System.out.println("\n--- Recherche d'un séjour ---");
        String ville = texte("Ville : ");
        LocalDate arrivee = date("Arrivée (jj/mm/aaaa) : ");
        LocalDate depart = date("Départ (jj/mm/aaaa) : ");
        while (!depart.isAfter(arrivee)) {
            System.out.println("  ! Le départ doit être postérieur à l'arrivée.");
            depart = date("Départ (jj/mm/aaaa) : ");
        }
        int personnes = entier("Nombre de personnes : ", 1, 20);
        int etoiles = entierParDefaut("Étoiles minimum, 0 à 5 [Entrée = 0] : ", 0, 5, 0);
        BigDecimal prixMax = montantOptionnel("Prix maximum en EUR [Entrée = sans limite] : ");

        long debut = System.nanoTime();
        Resultat r = agence.rechercher(new Critere(ville, arrivee, depart, personnes, etoiles, prixMax));
        long ms = (System.nanoTime() - debut) / 1_000_000;

        System.out.println("(recherche terminée en " + ms + " ms)");
        r.incidents().forEach(i -> System.out.println("/!\\ " + i));
        offres = r.offres();
        if (offres.isEmpty()) System.out.println("Aucune offre ne correspond à ces critères.");
        else afficherOffres();
    }

    private void consulter() {
        if (offres.isEmpty()) System.out.println("Aucune offre en mémoire : lancez d'abord une recherche.");
        else afficherOffres();
    }

    private void reserver() {
        if (offres.isEmpty()) {
            System.out.println("Aucune offre à réserver : lancez d'abord une recherche.");
            return;
        }
        System.out.println("\n--- Réservation ---");
        afficherOffres();
        int numero = entier("Numéro de l'offre (0 = annuler) : ", 0, offres.size());
        if (numero == 0) return;
        OffreAgence offre = offres.get(numero - 1);

        Voyageur voyageur = new Voyageur(texte("Nom du voyageur : "), texte("Prénom du voyageur : "));
        System.out.println("Coordonnées de paiement (fictives) : jamais réaffichées ni conservées.");
        Carte carte = lireCarte();

        System.out.println("Récapitulatif : " + offre.hotel() + ", " + offre.chambre() + ", du "
                + offre.arrivee().format(DATE) + " au " + offre.depart().format(DATE) + ", "
                + prix(offre.prixTotal()) + " EUR, " + voyageur.prenom() + " " + voyageur.nom()
                + ", carte " + carte.numeroMasque());
        if (!confirmer("Confirmer ? (o/n) : ")) {
            System.out.println("Réservation annulée.");
            return;
        }

        String idDemande = AgenceService.nouvelIdDemande();
        while (true) {
            try {
                Reservation c = agence.reserver(idDemande, offre, voyageur, carte);
                System.out.println("\nRéservation confirmée !");
                System.out.println("  Référence : " + c.reference());
                System.out.println("  Hôtel     : " + c.hotel() + ", chambre " + c.chambre());
                System.out.println("  Séjour    : du " + c.arrivee().format(DATE) + " au " + c.depart().format(DATE));
                System.out.println("  Prix      : " + prix(c.prixTotal()) + " EUR (prix confirmé = prix annoncé : "
                        + (c.prixTotal().compareTo(offre.prixTotal()) == 0 ? "oui" : "NON") + ")");
                System.out.println("  Carte     : " + carte.numeroMasque());
                if (c.rejeu()) System.out.println("  (demande déjà enregistrée : pas de seconde réservation)");
                retirer(offre);
                return;
            } catch (RefusException e) {
                System.out.println("  ! " + e.explication());
                if (e.motif() == RefusException.Motif.OFFRE_INDISPONIBLE) retirer(offre);
                return;
            } catch (PanneException e) {
                System.out.println("  ! " + e.getMessage() + ". La réservation n'est peut-être pas passée.");
                if (!confirmer("Réessayer la même demande ? (o/n) : ")) return;
            }
        }
    }

    private Carte lireCarte() {
        String numero;
        while (!(numero = secret("Numéro de carte : ").replaceAll("[\\s-]", "")).matches("\\d{13,19}"))
            System.out.println("  ! 13 à 19 chiffres attendus.");
        int mois = entier("Mois d'expiration (1-12) : ", 1, 12);
        int annee = entier("Année d'expiration (ex. 2030) : ", 2000, 2200);
        String cvc;
        while (!(cvc = secret("CVC : ")).matches("\\d{3,4}"))
            System.out.println("  ! 3 ou 4 chiffres attendus.");
        return new Carte(numero, mois, annee, cvc);
    }


    private void afficherOffres() {
        System.out.println(offres.size() + " offre(s), de la moins chère à la plus chère :");
        int n = 1;
        for (OffreAgence o : offres) {
            System.out.printf("%3d | %-10s | %-11s | %d* | %d pers. | %s -> %s | %9s EUR%n",
                    n++, o.hotel(), o.chambre(), o.etoiles(), o.capacite(),
                    o.arrivee().format(DATE), o.depart().format(DATE), prix(o.prixTotal()));
        }
    }

    private void retirer(OffreAgence offre) {
        offres = offres.stream().filter(o -> !o.equals(offre)).toList();
    }

    private static String prix(BigDecimal p) {
        return p.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }


    private String ligne(String invite) {
        System.out.print(invite);
        System.out.flush();
        try {
            String l = in.readLine();
            if (l == null) throw new FinDeSaisie();
            return l.trim();
        } catch (IOException e) {
            throw new FinDeSaisie();
        }
    }

    private String texte(String invite) {
        while (true) {
            String s = ligne(invite);
            if (!s.isEmpty()) return s;
            System.out.println("  ! Ce champ ne peut pas être vide.");
        }
    }

    private int entier(String invite, int min, int max) {
        while (true) {
            try {
                int n = Integer.parseInt(ligne(invite));
                if (n >= min && n <= max) return n;
            } catch (NumberFormatException ignore) {}
            System.out.println("  ! Entrez un entier entre " + min + " et " + max + ".");
        }
    }

    private int entierParDefaut(String invite, int min, int max, int defaut) {
        while (true) {
            String s = ligne(invite);
            if (s.isEmpty()) return defaut;
            try {
                int n = Integer.parseInt(s);
                if (n >= min && n <= max) return n;
            } catch (NumberFormatException ignore) {}
            System.out.println("  ! Entrez un entier entre " + min + " et " + max + ", ou Entrée.");
        }
    }

    private LocalDate date(String invite) {
        while (true) {
            try {
                return LocalDate.parse(ligne(invite), DATE);
            } catch (DateTimeParseException e) {
                System.out.println("  ! Date invalide. Format attendu : jj/mm/aaaa (ex. 10/12/2026).");
            }
        }
    }

    private BigDecimal montantOptionnel(String invite) {
        while (true) {
            String s = ligne(invite);
            if (s.isEmpty()) return null;
            try {
                BigDecimal m = new BigDecimal(s.replace(',', '.'));
                if (m.signum() > 0) return m;
            } catch (NumberFormatException ignore) {}
            System.out.println("  ! Entrez un montant positif (ex. 400 ou 399.90), ou Entrée.");
        }
    }

    private boolean confirmer(String question) {
        while (true) {
            String s = ligne(question).toLowerCase();
            if (s.equals("o") || s.equals("oui")) return true;
            if (s.equals("n") || s.equals("non")) return false;
            System.out.println("  ! Répondez par o ou n.");
        }
    }

    private String secret(String invite) {
        Console c = System.console();
        if (c == null) return ligne(invite);
        char[] t = c.readPassword("%s", invite);
        if (t == null) throw new FinDeSaisie();
        String s = new String(t).trim();
        Arrays.fill(t, ' ');
        return s;
    }
}