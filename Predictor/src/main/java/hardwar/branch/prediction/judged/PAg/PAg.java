package hardwar.branch.prediction.judged.PAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAg implements BranchPredictor {
	private final ShiftRegister SC; // saturating counter register
	private final RegisterBank PABHR; // per address branch history register
	private final Cache<Bit[], Bit[]> PHT; // page history table

	public PAg() {
		this(4, 2, 8);
	}

	/**
	 * Creates a new PAg predictor with the given BHR register size and initializes the PABHR based on
	 * the branch instruction size and BHR size
	 *
	 * @param BHRSize               the size of the BHR register
	 * @param SCSize                the size of the register which hold the saturating counter value
	 * @param branchInstructionSize the number of bits which is used for saving a branch instruction
	 */
	public PAg(int BHRSize, int SCSize, int branchInstructionSize) {
		// TODO: complete the constructor
		// Initialize the PABHR with the given bhr and branch instruction size
		PABHR = new RegisterBank(branchInstructionSize, BHRSize);

		// Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
		PHT = new PageHistoryTable(1 << BHRSize, SCSize);

		// Initialize the SC register
		SC = new SIPORegister("SC", SCSize, null);
	}

	/**
	 * @param instruction the branch instruction
	 * @return the predicted outcome of the branch instruction (taken or not taken)
	 */
	@Override
	public BranchResult predict(BranchInstruction instruction) {
		// TODO: complete Task 1
		ShiftRegister BHR = PABHR.read(instruction.getInstructionAddress());
		SC.load(PHT.setDefault(BHR.read(), getDefaultBlock()));
		return BranchResult.of(SC.read()[0].getValue());
	}

	/**
	 * @param instruction the branch instruction
	 * @param actual      the actual result of branch (taken or not)
	 */
	@Override
	public void update(BranchInstruction instruction, BranchResult actual) {
		// TODO: complete Task 2
		ShiftRegister BHR = PABHR.read(instruction.getInstructionAddress());
		Bit[] newData = CombinationalLogic.count(SC.read(), BranchResult.isTaken(actual), CountMode.SATURATING);
		PHT.put(BHR.read(), newData);
		BHR.insert(Bit.of(BranchResult.isTaken(actual)));
		PABHR.write(instruction.getInstructionAddress(), BHR.read());
	}

	/**
	 * @return a zero series of bits as default value of cache block
	 */
	private Bit[] getDefaultBlock() {
		Bit[] defaultBlock = new Bit[SC.getLength()];
		Arrays.fill(defaultBlock, Bit.ZERO);
		return defaultBlock;
	}

	@Override
	public String monitor() {
		return "PAg predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PHT.monitor();
	}
}
