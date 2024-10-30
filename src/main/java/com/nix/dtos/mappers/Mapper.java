package com.nix.dtos.mappers;

import java.util.List;

public interface Mapper<E, D> {

	public D mapToDTO(E e);

	public List<D> mapToDTOs(List<E> entities);
}
