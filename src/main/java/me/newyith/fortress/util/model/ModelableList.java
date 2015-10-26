package me.newyith.fortress.util.model;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class ModelableList<T extends Modelable> extends AbstractList<T> implements Modelable {
	public static class Model extends BaseModel {
		public List<BaseModel> list = new ArrayList<>();

		public Model(List<Modelable> list) {
			for (Modelable m : list) {
				this.list.add(m.getModel());
			}
		}
	}
	private Model model;
	private List<T> list;

	public ModelableList(Model model) {
		this.model = model;
		this.list = new ArrayList<>();

	}

	@Override
	public BaseModel getModel() {
		return this.model;
	}

	//-----------------------------------------------------------------------

	@Override
	public T get(int index) {
		return list.get(index);
	}

	@Override
	public int size() {
		return list.size();
	}


}
