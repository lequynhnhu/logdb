package org.araqne.logdb;

import java.util.List;
import java.util.Set;

public interface ProcedureRegistry {
	boolean isGranted(String procedureName, String loginName);

	Set<String> getProcedureNames();

	List<Procedure> getProcedures();

	Procedure getProcedure(String name);

	void createProcedure(Procedure procedure);

	/**
	 * @since 2.6.0
	 */
	void updateProcedure(Procedure procedure);

	void removeProcedure(String name);
}
