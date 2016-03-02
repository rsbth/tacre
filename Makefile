file= literature# Name of the root file

default: build delete

build:  main bib main

main:
	pdflatex $(file).tex

bib:
	bibtex $(file)
	pdflatex $(file).tex # Keep it here
	pdflatex $(file).tex # This aswell

show:
	@evince $(file).pdf&

delete:
	rm -f $(file).aux $(file).bbl $(file).blg $(file).log $(file).out $(file).toc

clean:
	make delete
	rm -f $(file).pdf
