import org.cloudbus.cloudsim.Cloudlet;

public class VmSort implements java.util.Comparator<NewVM> {
    @Override
    public int compare(NewVM a, NewVM b) {
        if (a.totalLength > b.totalLength)
            return 1;
        else if(a.totalLength < b.totalLength)
            return -1;
        else
            return 0;
    }
}