import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.*;

public class SS {
    private static final int NUM_CLOUDLETS_1 = 24;
    private static final int NUM_CLOUDLETS_2 = 36;
    private static final int NUM_VMS = 4;
    private static final int VM_PES = 1;
    private static final int CLOUDLET_LENGTH_CEILING = 1000;
    private static final int MIN_MIPS = 1000;
    private static final int MAX_MIPS = 2000;
    private static final int MIN_RAM = 2048;
    private static final int MAX_RAM = 4096;
    private static final int MIN_DISK = 1000000;
    private static final int MAX_DISK = 2000000;
    private static final int CLOUDLET_FILE_SIZE = 500;

    public static void main(String[] args) {
        Log.printLine("Starting CloudSimExample...");

        try {
            // Initialize CloudSim
            int numBrokers = 1;
            boolean traceFlag = false;
            CloudSim.init(numBrokers, Calendar.getInstance(), traceFlag);

            // Create Datacenter
            createDatacenter(createHostList());

            // Create Broker
            DatacenterBroker broker = loadBalancing();

            // Generate cloudlets with random execution lengths

            List<Cloudlet> cloudletList_1 = generateCloudlets(broker.getId(), NUM_CLOUDLETS_1);

            // Submit cloudlets to broker
            broker.submitCloudletList(cloudletList_1);

            // Create VMs and submit to broker
            List<Vm> vmList = createVms(broker.getId());
            broker.submitVmList(vmList);

            // Start the simulation
            CloudSim.startSimulation();

            // Retrieve the completed cloudlets from the broker
            List<Cloudlet> completedCloudlets = broker.getCloudletReceivedList();

            // Print the execution results
            printResult(completedCloudlets);

            // Stop the simulation
            CloudSim.stopSimulation();

            Log.printLine("First List finished!");

            CloudSim.init(numBrokers, Calendar.getInstance(), traceFlag);
            createDatacenter(createHostList());

            // Create Broker
            DatacenterBroker broker_2 = loadBalancing();


            // Generate cloudlets with random execution lengths
            assert broker_2 != null;
            List<Cloudlet> cloudletList_2 = generateCloudlets(broker_2.getId(), NUM_CLOUDLETS_2);

            broker_2.submitCloudletList(cloudletList_2);

            // Create VMs and submit to broker
            List<Vm> vmList_2 = createVms(broker_2.getId());
            broker_2.submitVmList(vmList_2);

            // Start the simulation
            CloudSim.startSimulation();

            // Retrieve the completed cloudlets from the broker
            List<Cloudlet> completedCloudlets_2 = broker_2.getCloudletReceivedList();

            // Print the execution results
            printResult(completedCloudlets_2);

            // Stop the simulation
            CloudSim.stopSimulation();

            Log.printLine("First List finished!");
        } catch (Exception e) {
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static DatacenterBroker loadBalancing() {
        try {
            return new DatacenterBroker("Broker") {
                @Override
                protected void submitCloudlets() {
                    // Create a map to track total processing time for each VM
                    List<NewVM> vmProcessingTimes = new ArrayList<>();
                    for (Vm vm : getVmsCreatedList()) {
                        vmProcessingTimes.add(new NewVM((vm)));
                    }

                    for (Cloudlet cloudlet : getCloudletList()) {
                        Collections.sort(vmProcessingTimes, new VmSort());

                        Vm vm = vmProcessingTimes.get(0).vm;
                        cloudlet.setVmId(vm.getId());
                        sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
                        cloudletsSubmitted++;
                        vmProcessingTimes.get(0).totalLength += cloudlet.getCloudletLength();

                        Log.printLine("Submitted Cloudlet " + cloudlet.getCloudletId() + " to VM " + vm.getId());
                    }
                }
            };
        } catch (Exception e) {
            Log.printLine("Error creating a broker.");
            return null;
        }
    }


    private static void createDatacenter(List<Host> hostList) {
        // Here we are just creating one datacenter
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.1;
        double costPerBw = 0.1;

        // create storage
        LinkedList<Storage> storageList = new LinkedList<>();

        // create DatacenterCharacteristics object
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        // create a Datacenter object
        try {
            new Datacenter("Datacenter", characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Host> createHostList() {
        Random random = new Random();
        List<Host> hostList = new ArrayList<>(NUM_VMS);
        for (int i = 0; i < NUM_VMS; i++) {
            int cpu = MIN_MIPS + random.nextInt(MAX_MIPS - MIN_MIPS + 1);
            int ram = MIN_RAM + random.nextInt(MAX_RAM - MIN_RAM + 1);
            long disk = MIN_DISK + random.nextInt(MAX_DISK - MIN_DISK + 1);

            List<Pe> peList = new ArrayList<>(VM_PES);
            for (int j = 0; j < VM_PES; j++) {
                peList.add(new Pe(j, new PeProvisionerSimple(cpu)));
            }

            hostList.add(
                    new Host(
                            i,
                            new RamProvisionerSimple(ram),
                            new BwProvisionerSimple(10000),
                            disk,
                            peList,
                            new VmSchedulerTimeShared(peList)
                    )
            );
        }
        return hostList;
    }

    private static List<Vm> createVms(int brokerId) {
        List<Vm> vmList = new ArrayList<>(NUM_VMS);
        for (int i = 0; i < NUM_VMS; i++) {
            long size = 10000;
            int ram = 512;
            int mips = 250;
            long bw = 1000;
            Vm vm = new Vm(
                    i,
                    brokerId,
                    mips,
                    VM_PES,
                    ram,
                    bw,
                    size,
                    "Xen",
                    new CloudletSchedulerTimeShared()
            );
            vmList.add(vm);
        }
        return vmList;
    }

    private static List<Cloudlet> generateCloudlets(int brokerId, int numCloudlet) {
        Random random = new Random();
        List<Cloudlet> cloudletList = new ArrayList<>(numCloudlet);
        for (int i = 0; i < numCloudlet; i++) {
            int length = random.nextInt(CLOUDLET_LENGTH_CEILING) + 1;
            int fileSize = random.nextInt(CLOUDLET_FILE_SIZE) + 1;
            Cloudlet cloudlet = new Cloudlet(
                    i,
                    length,
                    VM_PES,
                    fileSize,
                    fileSize,
                    new UtilizationModelFull(),
                    new UtilizationModelFull(),
                    new UtilizationModelFull()
            );
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
        }
        return cloudletList;
    }

    private static void printResult(List<Cloudlet> list) {
        List list1;
        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "Data center ID" + indent +
                "VM ID" + indent + "Time" + indent +
                "Start Time" + indent + "Finish Time" + indent + "STATUS");

        DecimalFormat dft = new DecimalFormat("###.##");
        Log.printLine(String.format("%-13s%-16s%-10s%-7s%-12s%-13s%-11s",
                "----------", "----------------  ", "-------", "------", "------------  ", "-------------  ", "---------"));


        Collections.sort(list, new CloudletSort());

        for (Cloudlet cloudlet : list) {
            Log.print(String.format("%-20d", cloudlet.getCloudletId()));
            Log.print(String.format("%-14d", cloudlet.getResourceId()));
            Log.print(String.format("%-8d", cloudlet.getVmId()));
            Log.print(String.format("%-11s", dft.format(cloudlet.getActualCPUTime())));
            Log.print(String.format("%-14s", dft.format(cloudlet.getExecStartTime())));
            Log.print(String.format("%-11s", dft.format(cloudlet.getFinishTime())));

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.printLine(String.format("%-11s", "SUCCESS"));
            } else {
                Log.printLine();
            }
        }
    }
}
