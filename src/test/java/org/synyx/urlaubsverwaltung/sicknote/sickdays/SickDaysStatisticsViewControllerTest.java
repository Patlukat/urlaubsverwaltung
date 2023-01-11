package org.synyx.urlaubsverwaltung.sicknote.sickdays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.synyx.urlaubsverwaltung.csv.CSVFile;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.search.PageableSearchQuery;
import org.synyx.urlaubsverwaltung.web.DateFormatAware;
import org.synyx.urlaubsverwaltung.web.FilterPeriod;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.Locale.GERMAN;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class SickDaysStatisticsViewControllerTest {

    private SickDaysStatisticsViewController sut;

    @Mock
    private SickDaysStatisticsService sickDaysStatisticsService;
    @Mock
    private PersonService personService;
    @Mock
    private SickDaysDetailedStatisticsCsvExportService sickDaysDetailedStatisticsCsvExportService;
    @Mock
    private DateFormatAware dateFormatAware;

    private static Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        sut = new SickDaysStatisticsViewController(sickDaysStatisticsService, sickDaysDetailedStatisticsCsvExportService,
            personService, dateFormatAware, clock);
    }

    @Test
    void downloadCSVReturnsBadRequestIfPeriodNotTheSameYear() throws Exception {

        final LocalDate startDate = LocalDate.parse("2019-01-01");
        final LocalDate endDate = LocalDate.parse("2018-08-01");

        final String fromString = "01.01.2019";
        when(dateFormatAware.parse(fromString, GERMAN)).thenReturn(Optional.of(startDate));
        final String endString = "01.08.2018";
        when(dateFormatAware.parse(endString, GERMAN)).thenReturn(Optional.of(endDate));

        perform(get("/web/sickdays/statistics/download").locale(GERMAN)
            .param("from", fromString)
            .param("to", endString))
            .andExpect(status().isBadRequest());
    }

    @Test
    void downloadCSVSetsDownloadHeaders() throws Exception {

        final Person signedInUser = new Person();
        signedInUser.setId(1L);
        when(personService.getSignedInUser()).thenReturn(signedInUser);

        final String dateString = "2022-05-10";
        final LocalDate date = LocalDate.parse(dateString);
        final FilterPeriod filterPeriod = new FilterPeriod(date, date);

        final PageableSearchQuery pageableSearchQuery =
            new PageableSearchQuery(PageRequest.of(2, 50, Sort.by(Sort.Direction.ASC, "person.firstName")), "");

        when(dateFormatAware.parse(dateString, GERMAN)).thenReturn(Optional.of(date));
        when(dateFormatAware.parse(dateString, GERMAN)).thenReturn(Optional.of(date));

        when(sickDaysStatisticsService.getAll(signedInUser, date, date, pageableSearchQuery))
            .thenReturn(new PageImpl<>(List.of()));

        when(sickDaysDetailedStatisticsCsvExportService.generateCSV(filterPeriod, List.of()))
            .thenReturn(new CSVFile("filename.csv", new ByteArrayResource(new byte[]{})));

        perform(get("/web/sickdays/statistics/download")
            .locale(GERMAN)
            .param("from", dateString)
            .param("to", dateString)
            .param("page", "2")
            .param("size", "50")
            .param("query", "")
        )
            .andExpect(header().string("Content-disposition", "attachment; filename=\"filename.csv\""))
            .andExpect(header().string("Content-Type", "text/csv"));
    }

    @Test
    void downloadCSVWritesCSV() throws Exception {

        final Person signedInUser = new Person();
        signedInUser.setId(1L);
        when(personService.getSignedInUser()).thenReturn(signedInUser);

        final LocalDate startDate = LocalDate.parse("2019-01-01");
        final LocalDate endDate = LocalDate.parse("2019-08-01");
        final FilterPeriod filterPeriod = new FilterPeriod(startDate, endDate);

        final PageableSearchQuery pageableSearchQuery =
            new PageableSearchQuery(PageRequest.of(2, 50, Sort.by(Sort.Direction.ASC, "person.firstName")), "");

        final String fromString = "01.01.2019";
        when(dateFormatAware.parse(fromString, GERMAN)).thenReturn(Optional.of(startDate));
        final String endString = "01.08.2019";
        when(dateFormatAware.parse(endString, GERMAN)).thenReturn(Optional.of(endDate));

        when(sickDaysStatisticsService.getAll(signedInUser, startDate, endDate, pageableSearchQuery))
            .thenReturn(new PageImpl<>(List.of()));

        when(sickDaysDetailedStatisticsCsvExportService.generateCSV(filterPeriod, List.of()))
            .thenReturn(new CSVFile("filename.csv", new ByteArrayResource(new byte[]{})));

        perform(get("/web/sickdays/statistics/download")
            .locale(GERMAN)
            .param("from", fromString)
            .param("to", endString)
            .param("page", "2")
            .param("size", "50")
        )
            .andExpect(status().isOk());
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build()
            .perform(builder);
    }
}
