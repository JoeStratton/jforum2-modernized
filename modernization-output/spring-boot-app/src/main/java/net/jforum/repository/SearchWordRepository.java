package net.jforum.repository;

import net.jforum.entity.SearchWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SearchWordRepository extends JpaRepository<SearchWord, Long> {

    Optional<SearchWord> findByWord(String word);
}