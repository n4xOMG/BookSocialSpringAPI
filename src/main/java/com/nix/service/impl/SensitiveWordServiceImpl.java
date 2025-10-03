package com.nix.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nix.models.SensitiveWord;
import com.nix.repository.SensitiveWordRepository;
import com.nix.service.SensitiveWordService;
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService{

	@Autowired
	SensitiveWordRepository sensitiveWordRepo;
	
	@Override
	public SensitiveWord findWordById(Integer wordId) throws Exception {
		Optional<SensitiveWord> word = sensitiveWordRepo.findById(wordId);
		
		if (word==null) {
			throw new Exception("No word found!");
		}
		return word.get();
		
	}

	@Override
	public List<SensitiveWord> findWordByName(String name) throws Exception {
		return sensitiveWordRepo.findByWord(name);
	}

	@Override
	public List<SensitiveWord> findAllWords() throws Exception {
		return sensitiveWordRepo.findAll();
	}

	@Override
	public SensitiveWord addNewWord(SensitiveWord word) throws Exception {
		SensitiveWord newWord = new SensitiveWord();
		newWord.setWord(word.getWord());
		
		return sensitiveWordRepo.save(newWord);
	}

	@Override
	public SensitiveWord editNewWord(Integer wordId, SensitiveWord word) throws Exception {
		SensitiveWord editWord = findWordById(wordId);
		editWord.setWord(word.getWord());
		
		return sensitiveWordRepo.save(editWord);
	}

	@Override
	public String deleteWord(Integer wordId) throws Exception {
		SensitiveWord deleteWord = findWordById(wordId);
		try {
			sensitiveWordRepo.delete(deleteWord);
			return "Word deleted";
		}
		catch(Exception e) {
			throw new Exception("Error deleting word: " + e);
		}
	}

}
