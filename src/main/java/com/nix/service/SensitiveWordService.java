package com.nix.service;

import java.util.List;

import com.nix.models.SensitiveWord;

public interface SensitiveWordService {
	public SensitiveWord findWordById(Integer wordId) throws Exception;

	public List<SensitiveWord> findWordByName(String name) throws Exception;

	public List<SensitiveWord> findAllWords() throws Exception;

	public SensitiveWord addNewWord(SensitiveWord word) throws Exception;

	public SensitiveWord editNewWord(Integer wordId, SensitiveWord word) throws Exception;

	public String deleteWord(Integer wordId) throws Exception;

}
