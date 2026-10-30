package hai704i.tp2.agence;

import hai704i.tp2.agence.generated.DemandeRecherche;
import hai704i.tp2.agence.generated.HotelPort;
import hai704i.tp2.agence.generated.HotelService;
import hai704i.tp2.agence.generated.Offre;
import jakarta.xml.ws.BindingProvider;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.util.List;

public class AgenceMain {
    public static void main(String[] args) throws Exception {
        String adresse = args.length > 0 ? args[0] : "http://localhost:8081/hotel";
        String agence = args.length > 1 ? args[1] : "AG-01";

        HotelPort port = new HotelService().getHotelSoapPort();
        ((BindingProvider) port).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, adresse);

        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        DemandeRecherche demandeRecherche = new DemandeRecherche();
        demandeRecherche.setAgence(agence);
        demandeRecherche.setVille("Montpellier");
        demandeRecherche.setArrivee(datatypeFactory.newXMLGregorianCalendar("2026-12-10"));
        demandeRecherche.setDepart(datatypeFactory.newXMLGregorianCalendar("2026-12-13"));
        demandeRecherche.setPersonnes(2);
        demandeRecherche.setEtoilesMin(3);
        demandeRecherche.setPrixMax(new BigDecimal("400.00"));

        List<Offre> offres = port.chercherOffres(demandeRecherche);
        for (Offre offre : offres) {
            System.out.println(
                    offre.getId()
                            + " : "
                            + offre.getHotel()
                            + " / "
                            + offre.getChambre()
                            + " / " + offre.getPrixTotal()
                            + " EUR"
            );
        }
    }
}