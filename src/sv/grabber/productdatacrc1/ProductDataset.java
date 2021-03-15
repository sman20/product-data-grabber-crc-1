package sv.grabber.productdatacrc1;

import java.util.Arrays;

/**
 * Implementation of product data set to store product search configuration.
 */
public class ProductDataset {
    private String name;
    private String link;
    private String keyPar;
    private String[] parNames;
    private String[] sizes;

    ProductDataset(String name, String keyPar, String link, String[] sizes, String[] parNames) {
        this.name = name;
        this.keyPar = keyPar;
        this.link = link;
        this.parNames = parNames;
        this.sizes = sizes;
    }

    @Override
    public String toString() {
        return name + "|" + keyPar + "|" + link + "|" + Arrays.toString(parNames) + "|" + Arrays.toString(sizes);
    }

    String getName() {
        return name;
    }

    String getLink() {
        return link;
    }

    String[] getParNames() {
        return parNames;
    }

    String[] getSizes() {
        return sizes;
    }
}
