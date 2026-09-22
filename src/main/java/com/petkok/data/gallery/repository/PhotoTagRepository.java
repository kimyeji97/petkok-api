package com.petkok.data.gallery.repository;

import com.petkok.data.gallery.entity.PhotoTag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사진 자유 태그 저장소. */
public interface PhotoTagRepository extends JpaRepository<PhotoTag, UUID> {

  List<PhotoTag> findByPhotoId(UUID photoId);

  void deleteByPhotoId(UUID photoId);
}
