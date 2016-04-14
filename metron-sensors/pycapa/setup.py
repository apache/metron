import os
from setuptools import setup


def fread(filename):
    return open(os.path.join(os.path.dirname(__file__), filename)).read()


def get_version():
    return fread('VERSION')


setup(
    name='pycapa',
    version=get_version(),
    author='Apache Metron Team',
    author_email='user@metron.incubator.apache.org',
    description='Pycapa performs network packet capture as part of Apache Metron',
    long_description=fread('README.md'),
    entry_points = {
      "console_scripts" : [ 'pycapa = pycapa.pycapa_cli:main' ],
    },
    packages = [ 'pycapa' ]
)
