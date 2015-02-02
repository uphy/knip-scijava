package org.knime.knip.scijava.commands.adapter.basic;

import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.knip.scijava.commands.adapter.OutputAdapterPlugin;
import org.scijava.plugin.Plugin;

/**
 * Adapter for Integer to {@link IntCell}.
 * 
 * @author Jonathan Hale (University of Konstanz)
 * 
 */
@Plugin(type = OutputAdapterPlugin.class)
public class IntOutputAdapter implements
		OutputAdapterPlugin<Integer, IntCell> {

	@Override
	public IntCell createCell(Integer o) {
		return new IntCell(o);
	}

	@Override
	public Class<Integer> getSourceType() {
		return Integer.class;
	}

	@Override
	public DataType getDataCellType() {
		return IntCell.TYPE;
	}

}