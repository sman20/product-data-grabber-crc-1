package sv.grabber.productdatacrc1;

import java.util.Arrays;

/**
 * Implementation of product data set to store product configuration for search.
 */
public class ProductDataset {
    private String name;
    private String link;
    private String keyPar;
    private String[] parNames;
    private String[] sizes;

    public ProductDataset(String name, String keyPar, String link, String[] sizes, String[] parNames) {
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

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

    public String[] getParNames() {
        return parNames;
    }

    public String[] getSizes() {
        return sizes;
    }
}
