package hai704i.tp2.agence;

import java.util.List;

public class AgenceMain {
    private static final int DELAI_CONNEXION_MS = 2_000;
    private static final int DELAI_REPONSE_MS = 5_000;

    public static void main(String[] args) {
        String agence = args.length > 0 ? args[0] : "AG-01";
        String secret = args.length > 1 ? args[1] : System.getenv().getOrDefault("AGENCE_SECRET", "");
        List<String> adresses = args.length > 2
                ? List.of(args).subList(2, args.length)
                : List.of("http://localhost:8081/hotel", "http://localhost:8082/hotel");

        List<HotelClient> hotels = adresses.stream()
                .map(a -> new HotelClient(a, DELAI_CONNEXION_MS, DELAI_REPONSE_MS))
                .toList();

        new AgenceCLI(new AgenceService(agence, secret, hotels)).lancer();
    }
}