package com.gaokao.service;

import com.gaokao.entity.ScoreLine;
import com.gaokao.entity.UniversityScoreLine;
import com.gaokao.mapper.ScoreLineMapper;
import com.gaokao.mapper.UniversityScoreLineMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ScoreLineService {

    @Autowired
    private ScoreLineMapper scoreLineMapper;
    @Autowired
    private UniversityScoreLineMapper universityScoreLineMapper;

    public List<ScoreLine> findScoreLines(Long provinceId, Integer year, String batch, String subjectType) {
        return scoreLineMapper.findList(provinceId, year, batch, subjectType);
    }

    public void saveScoreLine(ScoreLine scoreLine) {
        if (scoreLine.getId() == null) {
            scoreLineMapper.insert(scoreLine);
        } else {
            scoreLineMapper.update(scoreLine);
        }
    }

    public void deleteScoreLine(Long id) {
        scoreLineMapper.deleteById(id);
    }

    public List<UniversityScoreLine> findUniversityScoreLines(Long universityId, Long provinceId, Integer year) {
        return universityScoreLineMapper.findList(universityId, provinceId, year);
    }

    public void saveUniversityScoreLine(UniversityScoreLine usl) {
        if (usl.getId() == null) {
            universityScoreLineMapper.insert(usl);
        } else {
            universityScoreLineMapper.update(usl);
        }
    }

    public void deleteUniversityScoreLine(Long id) {
        universityScoreLineMapper.deleteById(id);
    }
}